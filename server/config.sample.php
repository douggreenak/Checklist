<?php
// Copy this file to config.php on your web host and fill it in. Never commit config.php.
return [
    // On the same cPanel host as the database, use host=localhost.
    'dsn'           => 'mysql:host=localhost;dbname=Checklist;charset=utf8mb4',
    'db_user'       => 'Checklist',
    'db_pass'       => 'CHANGE_ME',
    // Make the app password hash with:  php -r 'echo password_hash("your password", PASSWORD_DEFAULT);'
    'password_hash' => 'CHANGE_ME',
    // Any long random string:  php -r 'echo bin2hex(random_bytes(32));'
    'token_secret'  => 'CHANGE_ME',
];
