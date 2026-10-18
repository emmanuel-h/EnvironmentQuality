package fr.mandarine.environmentquality.presentation

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.GeoPoint

/** Large communes shown on the map before the user has searched for anything. */
val ReferenceCommunes: List<Commune> = listOf(
    Commune("75056", "Paris", departmentCode = "75", departmentName = "Paris", centre = GeoPoint(48.8566, 2.3522)),
    Commune("59350", "Lille", departmentCode = "59", departmentName = "Nord", centre = GeoPoint(50.6292, 3.0573)),
    Commune("76540", "Rouen", departmentCode = "76", departmentName = "Seine-Maritime", centre = GeoPoint(49.4431, 1.0993)),
    Commune("14118", "Caen", departmentCode = "14", departmentName = "Calvados", centre = GeoPoint(49.1829, -0.3707)),
    Commune("29019", "Brest", departmentCode = "29", departmentName = "Finistère", centre = GeoPoint(48.3904, -4.4861)),
    Commune("35238", "Rennes", departmentCode = "35", departmentName = "Ille-et-Vilaine", centre = GeoPoint(48.1173, -1.6778)),
    Commune("44109", "Nantes", departmentCode = "44", departmentName = "Loire-Atlantique", centre = GeoPoint(47.2184, -1.5536)),
    Commune("45234", "Orléans", departmentCode = "45", departmentName = "Loiret", centre = GeoPoint(47.9030, 1.9093)),
    Commune("67482", "Strasbourg", departmentCode = "67", departmentName = "Bas-Rhin", centre = GeoPoint(48.5734, 7.7521)),
    Commune("57463", "Metz", departmentCode = "57", departmentName = "Moselle", centre = GeoPoint(49.1193, 6.1757)),
    Commune("21231", "Dijon", departmentCode = "21", departmentName = "Côte-d'Or", centre = GeoPoint(47.3220, 5.0415)),
    Commune("87085", "Limoges", departmentCode = "87", departmentName = "Haute-Vienne", centre = GeoPoint(45.8336, 1.2611)),
    Commune("63113", "Clermont-Ferrand", departmentCode = "63", departmentName = "Puy-de-Dôme", centre = GeoPoint(45.7772, 3.0870)),
    Commune("69123", "Lyon", departmentCode = "69", departmentName = "Rhône", centre = GeoPoint(45.7640, 4.8357)),
    Commune("38185", "Grenoble", departmentCode = "38", departmentName = "Isère", centre = GeoPoint(45.1885, 5.7245)),
    Commune("33063", "Bordeaux", departmentCode = "33", departmentName = "Gironde", centre = GeoPoint(44.8378, -0.5792)),
    Commune("31555", "Toulouse", departmentCode = "31", departmentName = "Haute-Garonne", centre = GeoPoint(43.6047, 1.4442)),
    Commune("34172", "Montpellier", departmentCode = "34", departmentName = "Hérault", centre = GeoPoint(43.6108, 3.8767)),
    Commune("13055", "Marseille", departmentCode = "13", departmentName = "Bouches-du-Rhône", centre = GeoPoint(43.2965, 5.3698)),
    Commune("06088", "Nice", departmentCode = "06", departmentName = "Alpes-Maritimes", centre = GeoPoint(43.7102, 7.2620)),
    Commune("2A004", "Ajaccio", departmentCode = "2A", departmentName = "Corse-du-Sud", centre = GeoPoint(41.9192, 8.7386)),
)
