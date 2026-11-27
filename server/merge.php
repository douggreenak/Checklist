<?php
// Merging of two copies of the checklist so several people can edit at once.
// A document is a JSON array of lists. Every field that can change carries the time (ms) it last changed,
// and the newer change wins field by field. Deleted tasks and lists leave a small tombstone so they stay deleted.

function cl_list_defaults(array $l): array {
    return [
        'id' => (string)($l['id'] ?? ''),
        'name' => is_string($l['name'] ?? null) ? $l['name'] : '',
        'showCompleted' => (bool)($l['showCompleted'] ?? false),
        'argb' => (int)($l['argb'] ?? 0),
        'lu' => (int)($l['lu'] ?? 0),
        'dead' => (int)($l['dead'] ?? 0),
        'del' => is_array($l['del'] ?? null) ? array_map('intval', $l['del']) : [],
        'items' => is_array($l['items'] ?? null) ? $l['items'] : [],
    ];
}

/** Gives every list and task an id and timestamps (older documents had none). Ids are derived from content so repeated upgrades agree. */
function cl_normalize(array $doc): array {
    $out = [];
    foreach ($doc as $l) {
        if (!is_array($l) || !isset($l['id'])) continue;
        $n = cl_list_defaults($l);
        $items = [];
        foreach ($n['items'] as $idx => $it) {
            if (!is_array($it)) continue;
            $t = is_string($it['t'] ?? null) ? $it['t'] : '';
            $items[] = [
                'i' => isset($it['i']) && $it['i'] !== '' ? (string)$it['i'] : 'm' . substr(sha1($n['id'] . '|' . $idx . '|' . $t), 0, 16),
                't' => $t, 'tu' => (int)($it['tu'] ?? 0),
                'c' => (bool)($it['c'] ?? false), 'cu' => (int)($it['cu'] ?? 0),
                'o' => (float)($it['o'] ?? $idx), 'ou' => (int)($it['ou'] ?? 0),
            ];
        }
        $n['items'] = $items;
        $out[] = $n;
    }
    return $out;
}

function cl_pick(array $a, array $b, string $field, string $stamp): array {
    return $b[$stamp] > $a[$stamp] ? [$b[$field], $b[$stamp]] : [$a[$field], $a[$stamp]];
}

function cl_merge_list(array $a, array $b): array {
    if ($a['dead'] || $b['dead']) {
        return ['id' => $a['id'], 'name' => '', 'showCompleted' => false, 'argb' => 0, 'lu' => 0, 'dead' => max($a['dead'], $b['dead']), 'del' => [], 'items' => []];
    }
    $meta = $b['lu'] > $a['lu'] ? $b : $a;
    $del = $a['del'];
    foreach ($b['del'] as $k => $ts) $del[$k] = max($del[$k] ?? 0, $ts);
    $items = [];
    foreach ($a['items'] as $it) $items[$it['i']] = $it;
    foreach ($b['items'] as $it) {
        if (!isset($items[$it['i']])) { $items[$it['i']] = $it; continue; }
        $x = $items[$it['i']];
        [$t, $tu] = cl_pick($x, $it, 't', 'tu');
        [$c, $cu] = cl_pick($x, $it, 'c', 'cu');
        [$o, $ou] = cl_pick($x, $it, 'o', 'ou');
        $items[$it['i']] = ['i' => $it['i'], 't' => $t, 'tu' => $tu, 'c' => $c, 'cu' => $cu, 'o' => $o, 'ou' => $ou];
    }
    foreach ($del as $k => $_) unset($items[$k]);
    $items = array_values($items);
    usort($items, fn($p, $q) => $p['o'] <=> $q['o'] ?: strcmp($p['i'], $q['i']));
    return ['id' => $a['id'], 'name' => $meta['name'], 'showCompleted' => $meta['showCompleted'], 'argb' => $meta['argb'],
            'lu' => max($a['lu'], $b['lu']), 'dead' => 0, 'del' => $del, 'items' => $items];
}

function cl_merge(array $server, array $client): array {
    $server = cl_normalize($server); $client = cl_normalize($client);
    $byId = []; $order = [];
    foreach ($server as $l) { $byId[$l['id']] = $l; $order[] = $l['id']; }
    foreach ($client as $l) {
        if (!isset($byId[$l['id']])) { $byId[$l['id']] = $l; $order[] = $l['id']; continue; }
        $byId[$l['id']] = cl_merge_list($byId[$l['id']], $l);
    }
    $out = [];
    foreach ($order as $id) $out[] = $byId[$id];
    return $out;
}
