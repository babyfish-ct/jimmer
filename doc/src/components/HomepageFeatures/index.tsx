import React, { ReactNode, useMemo } from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Admonition from '@theme/Admonition';
import Tabs from "@theme/Tabs";
import TabItem from "@theme/TabItem";
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import { ViewMore } from '../ViewMore';
import { DynamicJoinProblem } from './DynamicJoinProblem';
import { Communication } from './Communication';
import { Benchmark } from '../Benchmark';

export default function HomepageFeatures(props: { readonly detailViews?: DetailViews }): JSX.Element {

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

interface DetailViews {
  readonly objectFetcher?: ReactNode;
  readonly saveCommand?: ReactNode;
  readonly cache?: ReactNode;
  readonly dynamicJoin?: ReactNode;
}

type FeatureItem = {
  title: string;
  description: JSX.Element;
};

const DtoExplosion = require("@site/static/img/dto-explosion.png").default;
const JavaFetcherMp4 = require("@site/static/img/java-fetcher.mp4").default;
const KotlinFetcherMp4 = require("@site/static/img/kotlin-fetcher.mp4").default;
const Save = require("@site/static/img/save.png").default;
const ObjectCache = require("@site/static/img/object-cache.jpeg").default;
const AssociationCache = require("@site/static/img/association-cache.png").default;
const CalculatedCache = require("@site/static/img/calculated-cache.png").default;
const MultiViewCache = require("@site/static/img/multi-view-cache.png").default;
const Consistency = require("@site/static/img/consistency.jpg").default;

const OBJECT_FETCHER_ZH: ReactNode =
  <ViewMore buttonText="简要了解" title="查询任意形状的对象" variant="outlined">
    <Tabs groupId="language">
      <TabItem value="java" label="Java">
          <video width="100%" controls>
              <source src={JavaFetcherMp4} type="video/mp4"/>
              <div style={{padding: '1rem', fontSize: '2rem', color: 'red'}}>Your browser does not support the video tag.</div>
          </video>
      </TabItem>
      <TabItem value="kotlin" label="Kotlin">
          <video width="100%" controls>
              <source src={KotlinFetcherMp4} type="video/mp4"/>
              <div style={{padding: '1rem', fontSize: '2rem', color: 'red'}}>Your browser does not support the video tag.</div>
          </video>
      </TabItem>
    </Tabs>
  </ViewMore>;

const SAVE_COMMAND_ZH: ReactNode = 
  <ViewMore buttonText="简要了解" title="保存任意形状的对象" variant="outlined">
    <img src={Save}/>
    <ul>
      <li>
        <p>
          <b>右上角: </b>用户传入一个任意形状的数据结构，让Jimmer写入数据库。 
        </p>
        <p>
          这和其他ORM框架的save方法之间存在本质差异。
          以JPA/Hibernate为例，对象的普通属性是否需要被保存通过
          <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/Column.html#insertable--">Column.insertable</a>和
          <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/Column.html#updatable--">Column.updatable</a>控制，
          关联属性是否需要被保存通过
          <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/OneToOne.html#cascade--">OneToOne.cascade</a>、
          <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/ManyToOne.html#cascade--">ManyToOne.cascade</a>、
          <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/OneToMany.html#cascade--">OenToMany.cascade</a>和
          <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/ManyToMany.html#cascade--">ManyToOne.cascade</a>控制。
          然而，无论如何开发人员如何配置，JPA/Hibernate能够为你保存的数据结构的形状是固定的。
        </p>
        <p>
          Jimmer采用完全不同方法，被保存的Jimmer对象虽然是强类型的，但具备动态性 <i>(即, 不设置对象属性和把对象对象属性设置为null是完全同的两码事)</i>，
          被设置的属性会被保存，而未被设置的属性会被忽略，这样，就可以保存任意形状的数据结构。
        </p>
      </li>
      <li>
        <p>
          <b>左上角: </b>从数据库中查询已有的数据结构，用于和用户传入的新数据结构对比。
        </p>
        <p>
          用户传入什么形状的数据结构，就从数据查询什么形状的数据结构，新旧数据结构的形状完全一致。所以，查询成本和对比成本由用户传入的数据结构的复杂度决定。
        </p>
      </li>
      <li>
        <p>
          <b>下方: </b>对比新旧数据结构，找出DIFF并执行相应的SQL操作，让新旧数据一致：
        </p>
        <ul>
          <li>
            <span style={{color:"orange"}}>橙色部分</span>：对于在新旧数据结构中存在的实体对象，如果某些标量属性发生变化，修改数据
          </li>
          <li>
            <span style={{color:"blue"}}>蓝色部分</span>：对于在新旧数据结构中存在的实体对象，如果某些关联发生变化，修改关联
          </li>
          <li>
            <span style={{color:"green"}}>绿色部分</span>：对于在新数据结构中存在但在旧数据结构中不存在实体对象，插入数据并建立关联
          </li>
          <li>
            <span style={{color:"red"}}>红色部分</span>：对于在旧数据结构中存在但在新数据结构中不存在实体对象，对此对象进行脱钩，清除关联并有可能删除数据
          </li>
        </ul>
      </li>
    </ul>
    <Admonition type='tip'>
      <p>此功能的目的：把任意形状的数据结构作为一个整体，使用一行代码写入数据库，无论中间细节多复杂，都不用关心。</p>
      <p>如果你了解Web领域的<a href="https://react.dev/">React</a>或<a href="https://vuejs.org/">Vuew</a>，不难看出这个功能很像`Virtual DOM diff`。</p>
    </Admonition>
    &nbsp;
    &nbsp;
    &nbsp;
  </ViewMore>

const CONSISTENCY_ZH: ReactNode = 
  <ViewMore buttonText='简要了解' title='缓存一致性' variant='outlined'>
    <p><img src={Consistency}/></p>
    <p>
      <b>左侧</b>：修改数据库中的数据，将<code>Book-10</code>的外建字段<code>STORE_ID</code>从<code>2</code>修改为<code>1</code>
    </p>
    <p>
      <b>右侧</b>：此举会导致连锁反应，Jimmer会自动清理所有受影响的缓存，清理缓存的动作会在IDE中留下日志记录
      <div style={{padding: '1rem', marginLeft: '2rem'}}>
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>
                  被清理的缓存名
                </TableCell>
                <TableCell>
                  <span style={{whiteSpace: "nowrap"}}>缓存类型</span>
                </TableCell>
                <TableCell>
                  描述
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell>Book-10</TableCell>
                <TableCell>对象缓存</TableCell>
                <TableCell>对于被修改的书籍<code>Book-10</code>而言，其对象缓存失效</TableCell>
              </TableRow>
              <TableRow>
                <TableCell>Book.store-10</TableCell>
                <TableCell>关联缓存</TableCell>
                <TableCell>对于被修改的书籍<code>Book-10</code>而言，其many-to-one关联属性`Book.store`所对应的缓存失效</TableCell>
              </TableRow>
              <TableRow>
                <TableCell>BookStore.books-2</TableCell>
                <TableCell>关联缓存</TableCell>
                <TableCell>对于旧的父对象<code>BookStore-2</code>而言，其one-to-many关联属性`BookStore.books`所对应的缓存失效</TableCell>
              </TableRow>
              <TableRow>
                <TableCell style={{whiteSpace: "nowrap"}}>BookStore.newestBooks-2</TableCell>
                <TableCell>计算缓存</TableCell>
                <TableCell>对于旧的父对象<code>BookStore-2</code>而言，其关联型计算属性`BookStore.newestBooks`所对应的缓存失效</TableCell>
              </TableRow>
              <TableRow>
                <TableCell>BookStore.avgPrice-2</TableCell>
                <TableCell>计算缓存</TableCell>
                <TableCell>对于旧的父对象<code>BookStore-2</code>而言，其关统计型计算属性`BookStore.avgPrice`所对应的缓存失效</TableCell>
              </TableRow>
              <TableRow>
                <TableCell>BookStore.books-1</TableCell>
                <TableCell>关联缓存</TableCell>
                <TableCell>对于新的父对象<code>BookStore-1</code>而言，其one-to-many关联属性`BookStore.books`所对应的缓存失效</TableCell>
              </TableRow>
              <TableRow>
                <TableCell>BookStore.newestBooks-1</TableCell>
                <TableCell>计算缓存</TableCell>
                <TableCell>对于新的父对象<code>BookStore-1</code>而言，其关联型计算属性`BookStore.newestBooks`所对应的缓存失效</TableCell>
              </TableRow>
              <TableRow>
                <TableCell>BookStore.avgPrice-1</TableCell>
                <TableCell>计算缓存</TableCell>
                <TableCell>对于新的父对象<code>BookStore-1</code>而言，其关统计型计算属性`BookStore.avgPrice`所对应的缓存失效</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </div>
    </p>
  </ViewMore>;

const BENCHMARK_ZH =
  <ViewMore buttonText='简要了解' title='性能报告' variant='outlined'>
    <Benchmark type='OPS' locale='zh'/>
    <b>每秒操作次数</b>
    <ul>
      <li>
        <b>横坐标</b>: 每次从数据库中查询到的数据对象的数量。
      </li>
      <li>
        <b>纵坐标</b>: 表示每次操作耗时(微秒)。
      </li>
    </ul>
    <p>
      你也可以点击图标上方的<code>现实与原生JDBC坐标</code>来和原始的JDBC对比，这样，你会得到难以置信的结果并难免质疑其真实性。在性能指标相关的文档中，我们会对其会给予解释。
    </p>
  </ViewMore>;

const FEATURE_LIST_ZH: FeatureItem[] = [
  {
    title: '将RDBMS抽象为图数据库',
    description: (
      <>
        以任意形状的数据结构作为基本操作单元，甚至可处理包含自关联属性的无限深度数据结构，让开发人员脱离<ViewMore buttonText="DTO类型爆炸" variant="text"><img src={DtoExplosion}/></ViewMore>地狱
        <ul>
          <li>查询任意形状的数据结构：可以理解成ORM的GraphQL化{OBJECT_FETCHER_ZH}</li>
          <li>
            保存任意形状的数据结构：快速实现复杂表单保存业务{SAVE_COMMAND_ZH}
          </li>
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
        <li>
          不仅支持简单的<ViewMore buttonText="对象缓存" variant="text"><img src={ObjectCache}/></ViewMore>，还支持属性级缓存，
          包括<ViewMore buttonText="关联属性缓存" variant='text'><img src={AssociationCache}/></ViewMore>
          和<ViewMore buttonText="计算属性缓存" variant='text'><img src={CalculatedCache}/></ViewMore>。
          最终提供任意形状的数据结构的缓存能力，而非简单对象的缓存能力
        </li>
        <li>
          以权限系统为代表技术常常导致不同用户看到不同的数据集，因此，Jimmer支持<ViewMore buttonText='多视图缓存' variant='text'><img src={MultiViewCache}/></ViewMore>，让不同的用户看到不同的缓存
        </li>
        <li>强大的缓存一致性支持，开发人员专注于修改数据库即可，Jimmer自动保证缓存和数据库数据的一致性{CONSISTENCY_ZH}</li>
      </ul>
    ),
  },
  {
    title: '便捷的功能',
    description: (
      <ul>
        <li>
          <p>对于REST服务而言，为客户端 <i>(比如Web前端)</i> 生成客户端代码 <i>(比如TypeScript)</i>。</p>
          <p>
            服务端和客户端的编程模型不同。
            <ul>
              <li>服务端作为生产者，其编程模型经过了简化<b>(消除了DTO爆炸)</b>，大幅降低实现成本</li>
              <li>客户端作为消费者，采用完整的编程模型<b>(重现了DTO爆炸)</b>，使用起来具有良好的开发体验</li>
            </ul>
            这样，服务端和客户端都得到最适合自己的编程模型。<Communication/>
          </p>
        </li>
        <li>对于GraphQL服务而言，Jimmer允许开发人员用非常简单的方法去实现</li>
      </ul>
    ),
  },
  {
    title: '强类型SQL DSL',
    description: (
      <ul>
        <li>Jimmer的DSL并非是对SQL的机械翻译，其独创的`动态表连接`，填补<DynamicJoinProblem/>，无论多么复杂的动态查询都可以轻松书写</li>
        <li>支持混入Native SQL表达式，通用性SQL DSL和特定数据库的非标准功能并不冲突</li>
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
        充分优化，尽可能压缩除IO等待外的代码执行消耗。充分发挥Java21虚拟线程的潜力，支撑更高的吞吐{BENCHMARK_ZH}
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
        <li>Jimmer's DSL is not a mechanical translation of SQL, its innovative `dynamic table join` fills an <DynamicJoinProblem/>, making it easy to write even the most complex dynamic queries</li>
        <li>Supports mixing native SQL expressions, generic SQL DSL and non-standard features of specificied databases are not conflict</li>
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
