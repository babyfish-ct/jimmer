import useDocusaurusContext from "@docusaurus/useDocusaurusContext"

export function useZh(): boolean {
    const {i18n} = useDocusaurusContext();
    const locale = i18n.currentLocale;
    return locale === "zh" || locale === "zh_cn" || locale == "zh_CN";
}