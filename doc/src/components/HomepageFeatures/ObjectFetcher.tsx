import React, { FC, ReactNode, memo } from "react";
import Tabs from "@theme/Tabs";
import TabItem from "@theme/TabItem";
import { ViewMore } from "../ViewMore";
import { useZh } from "@site/src/util/use-zh";

export const ObjectFetcher : FC = memo(() => {
    const zh = useZh();
    return zh ? ZH : EN;
});

const JavaFetcherMp4 = require("@site/static/img/java-fetcher.mp4").default;
const KotlinFetcherMp4 = require("@site/static/img/kotlin-fetcher.mp4").default;

const ZH: ReactNode =
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

const EN: ReactNode =
  <ViewMore buttonText="A brief introduction" title="Querying data structures of any shape" variant="outlined">
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