import React, { FC, memo, ReactNode } from "react";
import Grid from '@mui/material/Grid';
import { Card, CardContent, Typography } from "@mui/material";

export const Feedback: FC = memo(() => {
    return (
        <Grid container spacing={2} alignItems="stretch">
            {
                ITEMS.map(item => (       
                    <Grid key={item.author} container direction="column" item xs={12} lg={6}>
                        <Card elevation={3} style={{height: '100%'}}>
                            <CardContent>
                                <Grid item container>
                                    <Grid item xs={3}><Typography variant="h5" component="div">{item.author}</Typography></Grid>
                                    <Grid item xs={9}><Typography variant="h6" component="div">{item.company}</Typography></Grid>
                                </Grid>
                            </CardContent>
                            <CardContent>
                                {item.content}
                            </CardContent>
                        </Card>
                    </Grid> 
                ))
            }
        </Grid>
    )
});

const ITEMS: ReadonlyArray<{
    readonly author: string,
    readonly company: string,
    readonly content: ReactNode
}> = [
    {
        author: "南暮",
        company: "北京首信科技",
        content: (
            <>
                <ul>
                    <li>动态对象无惧任何形状的DTO</li>
                    <li>不可变对象让你无视多线程安全问题可被随意共享</li>
                    <li>简单流畅的API带来极度舒适的开发体验</li>
                    <li>完全透明的缓存机制让你从繁琐的工作中解脱</li>
                </ul>
                所以，强烈推荐Jimmer，从此解放你的生产力，早早下班不是梦！
            </>
        )
    },
    {
        author: "海",
        company: "成都九洲电子信息股份有限公司",
        content: (
            <>
                动态对象的理念能使我们控制到对象的每一个属性，解决了DTO爆炸是最让人开心的，计算属性+缓存的功能非常棒！
            </>
        )
    },
    {
        author: "折翼天使",
        company: "杭州数字霍因科技有限公司",
        content: (
            <>
                <ul>
                    <li>极其简单流畅的隐式表连接机制</li>
                    <li>业务分析的时候不再需要基于联表难度考虑表划分</li>
                    <li>基于动态对象和Fetcher的字段级无限深度对象形状控制能力，使得接口输出时不再需要定义DTO</li>
                    <li>支持任意形状无限深度对象的保存指令，新增、插入时不再需要基于对象形状考虑保存逻辑，只关注构建对象本身即可</li>
                </ul>
            </>
        )
    },
    {
        author: "阿童木.z",
        company: "景尚旅业",
        content: (
            <>
                jimmer打破了传统认知，整合了mybatis和JPA的优点，具备强大、优雅、高性能、易使用等优秀特性。无限级联对象树太棒了，极力推荐尝试的创新特性！
            </>
        )
    },
    {
        author: "Elva",
        company: "南阳羲和科技信息有限公司",
        content: (
            <>
                Jimmer 通过强类型减少了写代码错误的概率，自动生成代码，极大地提高了开发效率，对于复杂结构的查询更加得心应手
            </>
        )
    },
    {
        author: "航海",
        company: "四海万联",
        content: (
            <ul>
                <li>隐含表连接和动态对象。可以快速的实现业务的需求</li>
                <li>全局的缓存解决方案，自动化一致性和开发透明性</li>
                <li>功能强大，收益相对学习成本的比值高</li>
                <li>能显著减少业务系统代码量</li>
            </ul>
        )
    },
    {
        author: "Leon",
        company: "个人开发者",
        content: (
            <ul>
                <li>强类型DSL + 比Exposed更丰富强大的功能 -&gt; 开发体验与效率最大化</li>
                <li>依赖于jimmer任意数据结构查询与保存能力，消灭了Mybatis手写sql的烦恼，同时还解决DTO爆炸的问题</li>
                <li>缓存一致与透明，减少大量繁杂工作</li>
            </ul>
        )
    }
];
