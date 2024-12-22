<?php
// Gestionnaire de tâches avancé avec base de données SQLite

// Configuration de la base de données
$dsn = 'sqlite:tasks.db';
try {
    $db = new PDO($dsn);
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Création de la table si elle n'existe pas
    $db->exec("CREATE TABLE IF NOT EXISTS tasks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        description TEXT,
        status TEXT CHECK(status IN ('pending', 'completed')) DEFAULT 'pending',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )");
} catch (PDOException $e) {
    die("Erreur de connexion à la base de données: " . $e->getMessage());
}

// Ajouter une tâche
function addTask($db, $title, $description) {
    $stmt = $db->prepare("INSERT INTO tasks (title, description) VALUES (:title, :description)");
    $stmt->execute([':title' => $title, ':description' => $description]);
    echo "Tâche ajoutée avec succès !<br>";
}

// Lister les tâches
function listTasks($db) {
    $stmt = $db->query("SELECT * FROM tasks ORDER BY created_at DESC");
    $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "<ul>";
    foreach ($tasks as $task) {
        echo "<li><strong>" . htmlspecialchars($task['title']) . "</strong>: " .
             htmlspecialchars($task['description']) . " (" . $task['status'] . ") - " .
             $task['created_at'] . "</li>";
    }
    echo "</ul>";
}

// Marquer une tâche comme terminée
function completeTask($db, $id) {
    $stmt = $db->prepare("UPDATE tasks SET status = 'completed' WHERE id = :id");
    $stmt->execute([':id' => $id]);
    echo "Tâche marquée comme terminée !<br>";
}

// Supprimer une tâche
function deleteTask($db, $id) {
    $stmt = $db->prepare("DELETE FROM tasks WHERE id = :id");
    $stmt->execute([':id' => $id]);
    echo "Tâche supprimée avec succès !<br>";
}

// Exemple d'utilisation
//
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Gestionnaire de tâches</title>
</head>
<body>
    <h1>Gestionnaire de tâches</h1>

    <h2>Ajouter une tâche</h2>
    <form method="post">
        <input type="hidden" name="action" value="add">
        <label for="title">Titre:</label>
        <input type="text" id="title" name="title" required><br>
        <label for="description">Description:</label>
        <textarea id="description" name="description"></textarea><br>
        <button type="submit">Ajouter</button>
    </form>

    <h2>Liste des tâches</h2>
    <?php listTasks($db); ?>

    <h2>Actions sur une tâche</h2>
    <form method="post">
        <input type="hidden" name="action" value="complete">
        <label for="id">ID de la tâche à terminer:</label>
        <input type="number" id="id" name="id" required>
        <button type="submit">Terminer</button>
    </form>
    <form method="post">
        <input type="hidden" name="action" value="delete">
        <label for="id">ID de la tâche à supprimer:</label>
        <input type="number" id="id" name="id" required>
        <button type="submit">Supprimer</button>
    </form>
</body>
</html>
