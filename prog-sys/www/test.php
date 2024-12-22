<?php
// Affichage simple
echo "Bienvenue dans le test des fonctionnalités de PHP!\n\n";

// Variables et types
$nom = "PHP";
$version = 8.1;
$estActif = true;

// Affichage des variables
echo "Langage: $nom\nVersion: $version\nActif: " . ($estActif ? "Oui" : "Non") . "\n\n";

// Tableaux et boucles
$fruits = ["Pomme", "Banane", "Cerise"];
echo "Liste de fruits:\n";
foreach ($fruits as $fruit) {
    echo "- $fruit\n";
}

// Fonction simple
function addition($a, $b) {
    return $a + $b;
}

echo "\nAddition: 5 + 3 = " . addition(5, 3) . "\n\n";

// Classe et objet
class Animal {
    public $nom;

    public function __construct($nom) {
        $this->nom = $nom;
    }

    public function parler() {
        return "Je suis un $this->nom!";
    }
}

$animal = new Animal("chien");
echo $animal->parler() . "\n\n";

// Manipulation de fichiers
$fichier = "test.txt";
$contenu = "Ceci est un fichier généré par PHP.\n";

// Écriture dans un fichier
file_put_contents($fichier, $contenu);
echo "Fichier '$fichier' créé avec succès.\n";

// Lecture du fichier
$contenuLu = file_get_contents($fichier);
echo "Contenu du fichier: \n$contenuLu\n";

// Gestion des erreurs
try {
    if (!file_exists("inexistant.txt")) {
        throw new Exception("Le fichier inexistant.txt n'existe pas.");
    }
} catch (Exception $e) {
    echo "\nErreur: " . $e->getMessage() . "\n";
}

// Suppression du fichier pour nettoyer
unlink($fichier);
echo "Fichier '$fichier' supprimé après test.\n";

?>
