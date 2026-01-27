package com.example.netools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

/**
 * Classe utilitaire pour gérer la langue de l'application (Internationalisation).
 * Elle permet de changer la langue dynamiquement et de sauvegarder le choix de l'utilisateur.
 */
public class LanguageManager {

    // Nom du fichier de préférences (stockage interne léger type XML)
    private static final String PREFS_NAME = "prefs";
    // Clé pour retrouver la langue sauvegardée dans le fichier
    private static final String KEY_LANG = "lang_code";

    /**
     * Change la langue de l'application et sauvegarde le choix.
     * @param context Le contexte de l'application (nécessaire pour accéder aux ressources).
     * @param langCode Le code de la langue (ex: "fr" pour français, "en" pour anglais).
     */
    public static void setLocale(Context context, String langCode) {
        // 1. Création de l'objet Locale correspondant au code langue (fr, en, es...)
        Locale locale = new Locale(langCode);

        // 2. Définir cette locale comme celle par défaut pour l'instance Java
        Locale.setDefault(locale);

        // 3. Accès à la configuration globale de l'appareil pour cette app
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();

        // 4. Mise à jour de la configuration avec la nouvelle langue
        config.setLocale(locale);

        // 5. Application forcée de la nouvelle configuration
        // Note : updateConfiguration est déprécié sur les API récentes mais reste la méthode
        // standard compatible pour forcer le rafraîchissement immédiat des ressources (strings.xml).
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // 6. Sauvegarde persisante dans les SharedPreferences
        // Cela permet à l'appli de se souvenir de la langue au prochain redémarrage.
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_LANG, langCode);
        editor.apply(); // apply() est asynchrone (plus rapide que commit())
    }

    /**
     * Charge la langue sauvegardée et l'applique.
     * Cette méthode doit être appelée au démarrage de l'application (ex: dans onCreate de MainActivity).
     * @param context Le contexte pour accéder aux préférences.
     */
    public static void loadLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // On récupère la langue, ou "fr" par défaut si rien n'est trouvé
        String langCode = prefs.getString(KEY_LANG, "fr");
        // On applique la langue récupérée
        setLocale(context, langCode);
    }

    /**
     * Récupère simplement le code de la langue actuelle.
     * Utile pour cocher la bonne case dans les paramètres par exemple.
     * @return Le code langue (ex: "fr", "en").
     */
    public static String getLangCode(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANG, "fr");
    }
}