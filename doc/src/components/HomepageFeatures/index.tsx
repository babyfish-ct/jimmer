import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  Svg: React.ComponentType<React.ComponentProps<'svg'>>;
  description: JSX.Element;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Immutable data model',
    Svg: require('@site/static/img/immutable.svg').default,
    description: (
      <div style={{textAlign: 'left'}}>
        Powerful immutable object model
        by porting <a href="https://github.com/immerjs/immer">immer</a> to java/kotlin.
      </div>
    ),
  },
  {
    title: 'ORM for immutable data model',
    Svg: require('@site/static/img/orm.svg').default,
    description: (
      <div style={{textAlign: 'left'}}>
        <ul>
          <li>Include cache, no DTO required</li>
          <li>More powerful than other popular ORM frameworks</li>
          <li>Faster than other popular ORM frameworks</li>
        </ul>
      </div>
    ),
  },
  {
    title: 'Support Spring GraphQL',
    Svg: require('@site/static/img/spring.svg').default,
    description: (
      <div style={{textAlign: 'left'}}>
        Spring Boot 2.7.0 brings Spring GraphQL, jimmer has dedicated support for it to make development faster.
      </div>
    ),
  },
];

function Feature({title, Svg, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): JSX.Element {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
