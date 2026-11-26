<?php
// Checklist sync API. The Android app talks to this file; only this file talks to the database.
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

function respond(int $code, array $body): void {
    http_response_code($code);
    echo json_encode($body);
    exit;
}

function makeToken(string $secret): string {
    $exp = time() + 90 * 86400; // sign in again after 90 days
    return $exp . '.' . hash_hmac('sha256', (string)$exp, $secret);
}

function validToken(string $token, string $secret): bool {
    $p = explode('.', $token, 2);
    if (count($p) !== 2 || !ctype_digit($p[0]) || (int)$p[0] < time()) return false;
    return hash_equals(hash_hmac('sha256', $p[0], $secret), $p[1]);
}

function bearer(): string {
    $h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
    if ($h === '' && function_exists('getallheaders')) {
        foreach (getallheaders() as $k => $v) if (strtolower($k) === 'authorization') $h = $v;
    }
    return stripos($h, 'Bearer ') === 0 ? trim(substr($h, 7)) : '';
}

function lockedOut(PDO $pdo, string $ip): bool {
    $pdo->prepare('DELETE FROM checklist_login_failures WHERE at_time < ?')->execute([time() - 900]);
    $n = $pdo->prepare('SELECT COUNT(*) FROM checklist_login_failures WHERE ip = ?');
    $n->execute([$ip]);
    return (int)$n->fetchColumn() >= 5;
}

function recordFailure(PDO $pdo, string $ip): void {
    $pdo->prepare('INSERT INTO checklist_login_failures (ip, at_time) VALUES (?, ?)')->execute([$ip, time()]);
    usleep(400000);
}

/** A password changed from the app is stored in the database and overrides config.php. */
function currentHash(PDO $pdo, array $cfg): string {
    $q = $pdo->prepare('SELECT v FROM checklist_settings WHERE k = ?');
    $q->execute(['password_hash']);
    $v = $q->fetchColumn();
    return $v ? (string)$v : $cfg['password_hash'];
}

$cfg = require __DIR__ . '/config.php';

$raw = file_get_contents('php://input');
if (strlen($raw) > 2000000) respond(413, ['error' => 'Too large']);
$in = json_decode($raw ?: '{}', true);
if (!is_array($in)) $in = [];
$action = $_GET['action'] ?? '';

try {
    $pdo = new PDO($cfg['dsn'], $cfg['db_user'], $cfg['db_pass'], [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
    $pdo->exec('CREATE TABLE IF NOT EXISTS checklist_data (id INT NOT NULL PRIMARY KEY, body LONGTEXT NOT NULL, updated_at BIGINT NOT NULL)');
    $pdo->exec('CREATE TABLE IF NOT EXISTS checklist_login_failures (ip VARCHAR(45) NOT NULL, at_time BIGINT NOT NULL)');
    $pdo->exec('CREATE TABLE IF NOT EXISTS checklist_settings (k VARCHAR(50) NOT NULL PRIMARY KEY, v TEXT NOT NULL)');
    $ip = $_SERVER['REMOTE_ADDR'] ?? '';
    $hash = currentHash($pdo, $cfg);
    // Tokens are tied to the current password, so changing it signs every other device out.
    $secret = $cfg['token_secret'] . $hash;

    if ($action === 'login' && $_SERVER['REQUEST_METHOD'] === 'POST') {
        if (lockedOut($pdo, $ip)) respond(429, ['error' => 'Too many attempts. Try again in 15 minutes.']);
        $pw = $in['password'] ?? '';
        if (!is_string($pw) || !password_verify($pw, $hash)) {
            recordFailure($pdo, $ip);
            respond(401, ['error' => 'Wrong password']);
        }
        $pdo->prepare('DELETE FROM checklist_login_failures WHERE ip = ?')->execute([$ip]);
        respond(200, ['token' => makeToken($secret)]);
    }

    if (!validToken(bearer(), $secret)) respond(401, ['error' => 'Not signed in']);

    if ($action === 'changepw' && $_SERVER['REQUEST_METHOD'] === 'POST') {
        if (lockedOut($pdo, $ip)) respond(429, ['error' => 'Too many attempts. Try again in 15 minutes.']);
        $cur = $in['current'] ?? '';
        $new = $in['new'] ?? '';
        if (!is_string($cur) || !password_verify($cur, $hash)) {
            recordFailure($pdo, $ip);
            respond(403, ['error' => 'Current password is wrong']);
        }
        if (!is_string($new) || strlen($new) < 6 || strlen($new) > 200) {
            respond(400, ['error' => 'The new password must be at least 6 characters']);
        }
        $newHash = password_hash($new, PASSWORD_DEFAULT);
        $exists = $pdo->query("SELECT COUNT(*) FROM checklist_settings WHERE k = 'password_hash'")->fetchColumn() > 0;
        $pdo->prepare($exists
            ? "UPDATE checklist_settings SET v = ? WHERE k = 'password_hash'"
            : "INSERT INTO checklist_settings (v, k) VALUES (?, 'password_hash')")->execute([$newHash]);
        respond(200, ['token' => makeToken($cfg['token_secret'] . $newHash)]);
    }

    if ($action === 'get') {
        $r = $pdo->query('SELECT body, updated_at FROM checklist_data WHERE id = 1')->fetch(PDO::FETCH_ASSOC);
        respond(200, ['json' => $r ? $r['body'] : null, 'updatedAt' => $r ? (int)$r['updated_at'] : 0]);
    }

    if ($action === 'save' && $_SERVER['REQUEST_METHOD'] === 'POST') {
        $json = $in['json'] ?? null;
        if (!is_string($json) || !is_array(json_decode($json, true))) respond(400, ['error' => 'Bad data']);
        $now = (int)(microtime(true) * 1000);
        $exists = $pdo->query('SELECT COUNT(*) FROM checklist_data WHERE id = 1')->fetchColumn() > 0;
        $pdo->prepare($exists
            ? 'UPDATE checklist_data SET body = ?, updated_at = ? WHERE id = 1'
            : 'INSERT INTO checklist_data (body, updated_at, id) VALUES (?, ?, 1)')->execute([$json, $now]);
        respond(200, ['updatedAt' => $now]);
    }

    respond(404, ['error' => 'Unknown action']);
} catch (Throwable $e) {
    error_log('checklist api: ' . $e->getMessage());
    respond(500, ['error' => 'Server error']);
}
