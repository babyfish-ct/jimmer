import React, { ReactNode, useMemo } from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import { ViewMore } from '../ViewMore';
import { DynamicJoinProblem, DynamicJoinProblemPanel } from './DynamicJoinProblem';
import { Communication, CommunicationPanel } from './Communication';
import { ObjectFetcher, ObjectFetcherPanel } from './ObjectFetcher';
import { SaveCommand, SaveCommandPanel } from './SaveCommand';
import { CacheConsistency, CacheConsistencyPanel } from './CacheConsistency';
import { Performance, PerformancePanel } from './Performance';
import { DtoExplosion, ObjectCache, AssociationCache, CalculatedCache, MultiViewCache } from '../Image';
import Grid from '@mui/material/Grid';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

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
  title: ReactNode;
  description: JSX.Element;
};

const FEATURE_LIST_ZH: FeatureItem[] = [
  {
    title: <Grid container>
      <Grid item flex={1}>动静兼备的ORM框架</Grid>
      <Grid item>
        <ViewMore buttonText="快速预览" title="动静兼备的ORM框架">
          <Tabs groupId="homeDynamic">
            <TabItem value="query" label="查询任意形状数据结构">
              <ObjectFetcherPanel/>
            </TabItem>
            <TabItem value="save" label="保存任意形状数据结构">
              <SaveCommandPanel/>
            </TabItem>
            <TabItem value="join" label="动态表连接">
              <DynamicJoinProblemPanel/>
            </TabItem>
          </Tabs>
        </ViewMore>
      </Grid>
    </Grid>,
    description: (
      <>
        <ul>
          <li>将动态性引入静态语言ORM框架，兼具完备的编译期安全检查与灵活的动态复杂数据结构操作能力，消弥DTO爆炸，无惧工程拓展与需求变动。</li>
          <li>独创的SQL DSL支持动态表连接与 native SQL 植入，从容应对现实复杂性。</li>
        </ul>
      </>
    ),
  },
  {
    title: <Grid container>
    <Grid item flex={1}>强大易用的缓存机制</Grid>
    <Grid item>
      <ViewMore buttonText="快速预览" title="强大易用的缓存机制">
        <Tabs groupId="homeCache">
          <TabItem value="object" label="对象缓存">
            <ObjectCache/>
          </TabItem>
          <TabItem value="association" label="关联缓存">
            <AssociationCache/>
          </TabItem>
          <TabItem value="calculated" label="计算缓存">
            <CalculatedCache/>
          </TabItem>
          <TabItem value="multiview" label="多试图缓存">
            <MultiViewCache/>
          </TabItem>
          <TabItem value="consistance" label="缓存自动同步">
            <CacheConsistencyPanel/>
          </TabItem>
        </Tabs>
      </ViewMore>
    </Grid>
  </Grid>,
    description: (
      <>
        专注业务与收获缓存增益相得益彰。与具体缓存技术解耦的CDC方案对业务代码透明。
        <ul>
          <li>支持缓存自动同步。</li>
          <li>支持对象缓存、关联缓存、计算缓存等众多应用场景。</li>
          <li>完善的缓存多视角机制轻松对接权限需求。</li>
        </ul>
      </>
    ),
  },
  {
    title: <Grid container>
    <Grid item flex={1}>方便成熟的工程配套</Grid>
    <Grid item>
      <ViewMore buttonText="快速预览" title="客户端代码生成示范">
        <CommunicationPanel/>
      </ViewMore>
    </Grid>
  </Grid>,
    description: (
      <>
        提供一站式业务系统开发解决方案。
        <ul>
          <li>支持客户端代码生成。</li>
          <li>简化REST与GraphQL服务开发。</li>
          <li>支持跨微服务的数据关联。</li>
        </ul>
      </>
    ),
  },
  {
    title: <Grid container>
    <Grid item flex={1}>绝不妥协的极致性能</Grid>
    <Grid item>
      <ViewMore buttonText="快速预览" title="JDBC查询结果映射Benchmark">
        <PerformancePanel/>
      </ViewMore>
    </Grid>
  </Grid>,
    description: (
      <ul>
        <li>追求极致的细节优化，拒绝生成低性能SQL。</li>
        <li>强大的预编译技术保证了JDBC查询结果集到实体对象的高效映射。</li>
      </ul>
    ),
  },
  {
    title: '低廉可控的学习成本',
    description: (
      <>
        无需特殊前置知识，具备任何ORM使用经验的老手均可快速无痛迁移；学习曲线科学友好，是新人研习ORM应用的优质参考。
      </>
    ),
  },
  {
    title: '快速复用的代码资产',
    description: (
      <>
        动静结合的设计实现了关系型数据到局部图结构的更高层次抽象，高性能的一站式解决方案隔离了庞杂的技术性细节，为专注核心业务、快速积累高度可复用的代码资产提供强有力支撑。
      </>
    ),
  }
];

const FEATURE_LIST_EN: FeatureItem[] = [
  {
    title: <Grid container>
      <Grid item flex={1}>Both dynamism and static</Grid>
      <Grid item>
        <ViewMore buttonText="Quick view" title="ORM framework that combines dynamism and static typing">
          <Tabs groupId="homeDynamic">
            <TabItem value="query" label="Query data structure of arbitrary shape">
              <ObjectFetcherPanel/>
            </TabItem>
            <TabItem value="save" label="Save data structure of arbitrary shape">
              <SaveCommandPanel/>
            </TabItem>
            <TabItem value="join" label="Dynamic table joins">
              <DynamicJoinProblemPanel/>
            </TabItem>
          </Tabs>
        </ViewMore>
      </Grid>
    </Grid>,
    description: (
      <>
        <ul>
          <li>Brings dynamism into static language ORM frameworks, achieving flexible capabilities for operating on complex dynamic data structures without compromising the complete compile-time safety checks of static languages, eliminating DTO explosion and unafraid of engineering expansion and changing requirements.</li>
          <li>Its innovative SQL DSL supports dynamic table joins and native SQL embedding, gracefully handling real-world complexity.</li>
        </ul>
      </>
    ),
  },
  {
    title: <Grid container>
    <Grid item flex={1}>Powerful and easy cache</Grid>
    <Grid item>
      <ViewMore buttonText="Quick view" title="Powerful and easy cache">
        <Tabs groupId="homeCache">
          <TabItem value="object" label="Object Cache">
            <ObjectCache/>
          </TabItem>
          <TabItem value="association" label="Association Cache">
            <AssociationCache/>
          </TabItem>
          <TabItem value="calculated" label="Calculation Cache">
            <CalculatedCache/>
          </TabItem>
          <TabItem value="multiview" label="Multiview Cache">
            <MultiViewCache/>
          </TabItem>
          <TabItem value="consistance" label="Auto Cache Invalidation">
            <CacheConsistencyPanel/>
          </TabItem>
        </Tabs>
      </ViewMore>
    </Grid>
  </Grid>,
    description: (
      <>
        Focuses on business and reaping caching gains. CDC solutions decoupled from specific caching technologies are transparent to business code.
        <ul>
          <li>Automatic cache invalidation.</li>
          <li>Supports numerous application scenarios like object cache, association cache, calculation cache.</li>
          <li>Mature multi-view cache mechanisms easily integrate with permission requirements.</li>
        </ul>
      </>
    ),
  },
  {
    title: <Grid container>
    <Grid item flex={1}>Convenient project support</Grid>
    <Grid item>
      <ViewMore buttonText="Quick view" title="Client code generation demo">
        <CommunicationPanel/>
      </ViewMore>
    </Grid>
  </Grid>,
    description: (
      <>
        Provides a one-stop solution for business system development.
        <ul>
          <li>Supports client code generation.</li>
          <li>Simplifies REST and GraphQL service development.</li>
          <li>Supports cross-service data associations.</li>
        </ul>
      </>
    ),
  },
  {
    title: <Grid container>
    <Grid item flex={1}>Ultimate performance</Grid>
    <Grid item>
      <ViewMore buttonText="Quick view" title="JDBC Result mapping Benchmark">
        <PerformancePanel/>
      </ViewMore>
    </Grid>
  </Grid>,
    description: (
      <ul>
        <li>Pursues meticulous optimizations, rejecting low-performance SQL generation.</li>
        <li>Powerful precompilation technology ensures efficient mapping from JDBC result sets to entity objects.</li>
      </ul>
    ),
  },
  {
    title: 'Low and controllable learning costs',
    description: (
      <>
        Requires no special prior knowledge - veterans of any ORM can quickly and painlessly migrate. The learning curve is scientifically friendly, 
        serving as a high-quality reference for new learners of ORM usage.
      </>
    ),
  },
  {
    title: 'Rapid reuse of code assets',
    description: (
      <>
        The combination of static and dynamic design realizes a higher level of abstraction from relational data to local graph structures. 
        The high-performance one-stop solution isolates complex technical details, strongly supporting a focus on core business and rapid 
        accumulation of highly reusable code assets.
      </>
    ),
  }
];
