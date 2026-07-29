import i18n from "i18next";
import {initReactI18next} from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import HttpApi from "i18next-http-backend";

if (localStorage.getItem("language") === null)
    localStorage.setItem("language", navigator.language.split("-")[0]);

export const languages = [
    {code: "en", name: "English", imageCode: "gb"},
    {code: "de", name: "Deutsch", imageCode: "de"},
    {code: "da", name: "Dansk", imageCode: "dk"},
    {code: "es", name: "Español", imageCode: "es"},
    {code: "fr", name: "Français", imageCode: "fr"},
    {code: "ja", name: "日本語", imageCode: "jp"},
    {code: "pl", name: "Polski", imageCode: "pl"},
    {code: "nl", name: "Nederlands", imageCode: "nl"},
    {code: "vi", name: "Tiếng Việt", imageCode: "vi"},
]

i18n.use(initReactI18next).use(LanguageDetector).use(HttpApi).init({
    supportedLngs: languages.map((lang) => lang.code),
    fallbackLng: "en",
    backend: {
        loadPath: "/assets/locales/{{lng}}.json"
    },
    detection: {
        order: ["localStorage"],
        lookupLocalStorage: "language"
    }
});

export default i18n;
