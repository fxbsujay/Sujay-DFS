import { createI18n } from 'vue-i18n'
import { getLang, setLang} from '@/utils/cookies'
import ZH from './zh'
import EN from './en'
// $t('msg.title')
const msg = {
    en: {
       ...EN
    },
    zh: {
       ...ZH
    }
}

const getCurrentLanguage = () => {
    const UALang = navigator.language
    return getLang() ? getLang() : UALang.indexOf('zh') !== -1 ? 'zh' : 'en'
}

const i18n = createI18n({
    legacy: false,
    globalInjection: true,
    locale: getCurrentLanguage(),
    messages: msg
})

export default i18n