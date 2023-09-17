import React, { FC, ReactNode, memo } from "react";
import Tabs from "@theme/Tabs";
import TabItem from "@theme/TabItem";
import { ViewMore } from "../ViewMore";
import { useZh } from "@site/src/util/use-zh";
import { ViewDialog } from "../ViewDialog";
import Admonition from "@theme/Admonition";

export const ObjectFetcher : FC = memo(() => {
    const zh = useZh();
    if (zh) {
        return (
            <ViewMore buttonText="简要了解" title="查询任意形状的对象">
              {ZH}
            </ViewMore>
        );
    }
    return (
        <ViewMore buttonText="A brief introduction" title="Query data structure of any shape">
            {EN}
        </ViewMore>
    );
});

export const ObjectFetcherDialog : FC<{
    readonly open: boolean,
    readonly onClose: () => void
}> = memo((props) => {
    const zh = useZh();
    if (zh) {
        return (
            <ViewDialog title="查询任意形状的对象" {...props}>
              {ZH}
            </ViewDialog>
        );
    }
    return (
        <ViewDialog title="Query data structure of any shape" {...props}>
            {EN}
        </ViewDialog>
    );
});

export const ObjectFetcherPanel: FC = memo(() => {
    const zh = useZh();
    return zh ? ZH : EN;
});

const JavaFetcherMp4 = require("@site/static/img/java-fetcher.mp4").default;
const KotlinFetcherMp4 = require("@site/static/img/kotlin-fetcher.mp4").default;

const ZH: ReactNode =
    <>
        <Tabs groupId="language">
            <TabItem value="java" label="Java">
                <div style={{position: 'relative', width: '100%', paddingTop: '56.25%'}}>
                    <iframe 
                    src="//player.bilibili.com/player.html?bvid=BV1W14y167x7&page=1&high_quality=1&danmaku=0" 
                    scrolling="no" 
                    frameBorder="no" 
                    allowFullScreen={true}
                    style={{position: 'absolute', left: 0, top: 0, width: "100%", height: "100%"}}> 
                    </iframe>
                </div>
            </TabItem>
            <TabItem value="kotlin" label="Kotlin">
                <div style={{position: 'relative', width: '100%', paddingTop: '56.25%'}}>
                    <iframe 
                    src="//player.bilibili.com/player.html?bvid=BV1ic411F7hz&page=1&high_quality=1&danmaku=0" 
                    scrolling="no" 
                    frameBorder="no" 
                    allowFullScreen={true}
                    style={{position: 'absolute', left: 0, top: 0, width: "100%", height: "100%"}}> 
                    </iframe>
                </div>
            </TabItem>
        </Tabs>
        <Admonition type="info">
            和GraphQL比较
            <ul>
                <li>GraphQL基于HTTP服务，该功能只有在跨越HTTP服务的边界才能呈现；而在Jimmer中，这是ORM的基础API，你可以在任何代码逻辑中使用此能力。</li>
                <li>截止到目前为止，GraphQL协议不支持对深度无限的自关联属性的递归查询；而Jimer支持。</li>
            </ul>
        </Admonition>
    </>;

const EN: ReactNode =
    <>
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
        <Admonition type="info">
            Compare to GraphQL
            <ul>
                <li>GraphQL is based on HTTP services, which can only be experienced if it crosses the boundaries of HTTP services. In Jimmer, this is the underlying API for ORM, and you can use this capability in any code logic.</li>
                <li>Until now, the GraphQL protocol does not support recursive queries on self-associated properties with infinite depth; And Jimer supports</li>
            </ul>
        </Admonition>
    </>;