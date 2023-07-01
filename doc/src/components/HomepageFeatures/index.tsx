import React, { useMemo } from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

export default function HomepageFeatures(): JSX.Element {

  const {i18n} = useDocusaurusContext();

  const features = useMemo<ReadonlyArray<FeatureItem>>(() => {
    const locale = i18n.currentLocale;
    return locale === 'zh' || locale == 'zh_CN' || locale == 'zh_CN' ?
      FEATURE_LIST_ZH :
      FEATURE_LIST_EN;
  }, []);
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {features.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}

function Feature({title, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}

type FeatureItem = {
  title: string;
  description: JSX.Element;
};

const FEATURE_LIST_ZH: FeatureItem[] = [
  {
    title: '将RDBMS抽象为图数据库',
    description: (
      <>
        以任意形状的数据结构作为基本操作单元，甚至可处理包含自关联属性的无限深度数据结构，让开发人员脱离DTO类型爆炸地狱
        <ul>
          <li>查询任意形状的数据结构：可以理解成ORM的GraphQL化</li>
          <li>保存任意形状的数据结构：快速实现复杂表单保存业务</li>
        </ul>
      </>
    ),
  },
  {
    title: '强大的缓存支持',
    description: (
      <ul>
        <li>是否使用缓存，对开发人员完全透明</li>
        <li>使用哪些缓存技术，由开发人员决定</li>
        <li>不仅支持简单的`id-&gt;对象`缓存，还支持属性级缓存，包括关联属性缓存和计算属性缓存。最终提供任意形状的数据结构的缓存能力，而非简单对象的缓存能力</li>
        <li>强大的缓存一致性支持，开发人员专注于修改数据库即可，Jimmer自动保证缓存和数据库数据的一致性</li>
      </ul>
    ),
  },
  {
    title: '便捷的功能',
    description: (
      <ul>
        <li>
          对于REST服务而言，前后端免对接。此功能中前后端编程模型不同，但都使用对自己最有利的编程模型。
          <ul>
            <li>服务端开发很简单，因为Jimmer解决了DTO类型爆炸问题</li>
            <li>客户端使用体验高，在自动生成客户端代码中，DTO类型被恢复，所有API的返回类型都有精确定义</li>
          </ul>
        </li>
        <li>对于GraphQL服务而言，Jimmer允许开发人员用非常简单的方法去实现</li>
      </ul>
    ),
  },
  {
    title: '强类型SQL DSL',
    description: (
      <ul>
        <li>支持混入Native SQL表达式，通用性SQL DSL和特定数据库的非标准功能并不冲突</li>
        <li>Jimmer独创的`动态表连接`，填补行业空白，无论多么复杂的动态查询都可以轻松书写</li>
        <li>良好的代码安全性，编译时发现绝大部分问题，同时享受IDE智能提示支撑下的流畅开发体验</li>
      </ul>
    ),
  },
  {
    title: '良好的Spring支持',
    description: (
      <ul>
        <li>内置Spring Boot Starter支持，简化项目搭建</li>
        <li>支持Spring Data开发风格</li>
        <li>轻松结合Spring Cloud微服务技术体系和Jimmer远程实体关联</li>
      </ul>
    ),
  },
  {
    title: '极致的性能',
    description: (
      <>
        充分优化，尽可能压缩除IO等待外的代码执行消耗。充分发挥Java21虚拟线程的潜力，支撑更高的吞吐
      </>
    ),
  }
];

const FEATURE_LIST_EN: FeatureItem[] = [
  {
    title: 'Abstracting RDBMS into graph database',
    description: (
      <>
        Using data structures of any shape as the basic operation unit, 
        even handling infinite depth data structures with self-associated properties, 
        allows developers to escape the DTO explosion hell
        <ul>
          <li>Querying data structures of any shape: can be understood as GraphQLization of ORM</li>
          <li>Saving data structures of any shape: quickly implement complex form saving business</li>
        </ul>
      </>
    ),
  },
  {
    title: 'Powerful caching support',
    description: (
      <ul>
        <li>Whether to use caching is completely transparent to developers</li>
        <li>Which caching technologies to use is determined by developers</li>
        <li>
          Not only supports simple `id-&gt;object` cache, 
          but also supports property-level cache, including caching of association properties and calculated properties. 
          Ultimately, it provides caching capabilities for data structures of any shape, not just simple objects
        </li>
        <li>
          Powerful caching consistency support, developers can focus on modifying the database, 
          and Jimmer automatically ensures the consistency of cache and database data.
        </li>
      </ul>
    ),
  },
  {
    title: 'Convenient features',
    description: (
      <ul>
        <li>
        For REST services, there is no need for front-end and back-end integration. 
        In this feature, the front-end and back-end programming models are different, 
        but both use the programming model that is most advantageous to themselves
          <ul>
            <li>Server-side development is simple because Jimmer solves the issue of DTO type explosion</li>
            <li>
              The client-side experience is high. 
              In the automatically generated client code, DTO types are restored, 
              and the return types of all APIs are precisely defined
            </li>
          </ul>
        </li>
        <li>For GraphQL services, Jimmer allows developers to implement them in a very simple way</li>
      </ul>
    ),
  },
  {
    title: 'Strongly-typed SQL DSL',
    description: (
      <ul>
        <li>Supports mixing native SQL expressions, generic SQL DSL and non-standard features of specificied databases are not conflict</li>
        <li>Jimmer's innovative `dynamic table join` fills an industry gap, making it easy to write even the most complex dynamic queries</li>
        <li>
          Good code safety, with the ability to detect the majority of issues at compile time, 
          while enjoying a smooth development experience supported by intelligent IDE suggestions
        </li>
      </ul>
    ),
  },
  {
    title: 'Good Spring support',
    description: (
      <ul>
        <li>Built-in Spring Boot Starter support, simplifying project setup</li>
        <li>Supports Spring Data development style</li>
        <li>Easily integrates with the Spring Cloud microservices technology stack and remote entity associations of Jimmer</li>
      </ul>
    ),
  },
  {
    title: 'Ultimate performance',
    description: (
      <>
        Fully optimized to minimize code execution overhead excluding IO waiting. 
        Fully harnesses the potential of Java 21 virtual threads to support higher throughput
      </>
    ),
  }
];

