import React, { useEffect, useMemo, useState } from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import styles from './index.module.css';
import HomepageFeatures from '@site/src/components/HomepageFeatures';
import useIsBrowser from '@docusaurus/useIsBrowser';

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
  const isBrowser = useIsBrowser();
  return isBrowser ? <BrowserHome/> : <HomeCore/>;
}

function BrowserHome(): JSX.Element {
  const [isLoaded, setLoaded] = useState(true);
  useEffect(() => {
    if (document.body.addEventListener) {
      document.body.addEventListener("load", () => setLoaded(true));
    } else {
      (document.body as any).attachEvent("onLoad", () => setLoaded(true));
    }
  }, []);
  return (
    <>
      {
        !isLoaded && <div style={{position: "fixed", zIndex: 999, left: 0, top: 0, width: "100%", height: "100%", background: "black", color: "white", opacity: "0.5", paddingTop: "10rem", textAlign: "center", fontSize: "10rem"}}>
          Loading...
        </div>
      }
      <HomeCore/>
    </>
  );
}

function HomeCore(): JSX.Element {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} documentation`}
      description="Description will go into a meta tag in <head />">
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
    </Layout>
  );
}
