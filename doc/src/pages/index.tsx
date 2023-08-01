import React, { useMemo } from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import styles from './index.module.css';
import HomepageFeatures from '@site/src/components/HomepageFeatures';
import BrowserOnly from '@docusaurus/BrowserOnly';

function HomepageHeader() {
  const {siteConfig, i18n} = useDocusaurusContext();
  const isZh = useMemo(() => {
    const locale = i18n.currentLocale;
    return locale === 'zh' || locale == 'zh_CN' || locale == 'zh_cn';
  }, [i18n.currentLocale]);
  const title = useMemo(() => {
    return isZh ? "Jimmer, 针对Java和Kotlin的革命性ORM" : siteConfig.title;
  }, [isZh, siteConfig.title]);
  const tagline = useMemo(() => {
    return isZh ? "不只是ORM，还是基于它的集成化方案(包含强大的缓存管理机制)" : siteConfig.tagline;
  }, [isZh, siteConfig.tagline]);
  const tutorial = useMemo(() => {
    return isZh ? "进入教程" : "Goto Tutorial";
  }, [isZh]);
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <h1 className="hero__title">{title}</h1>
        <p className="hero__subtitle">{tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/overview/introduction">
              {tutorial}
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home(): JSX.Element {
  const {siteConfig} = useDocusaurusContext();
  return (
    <BrowserOnly>
      {() => (
        <Layout
        title={`${siteConfig.title} documentation`}
        description="Description will go into a meta tag in <head />">
        <HomepageHeader />
        <main>
          <HomepageFeatures />
        </main>
      </Layout>
      )}
    </BrowserOnly>
  );
}
