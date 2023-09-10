import React, { FC, memo } from "react";
import { ViewMore } from "../ViewMore";
import { Benchmark } from "../Benchmark";
import { useZh } from "@site/src/util/use-zh";
import { ViewDialog } from "../ViewDialog";

export const Performance: FC = memo(() => {
    const zh = useZh();
    return zh? 
        <ViewMore buttonText='简要了解' title='性能报告' variant='outlined'>
            {ZH}
        </ViewMore> : 
        <ViewMore buttonText='A Brief Introduction' title='Performance Report' variant='outlined'>
            {EN}
        </ViewMore>;
});

export const PerformanceDialog: FC<{
    readonly open: boolean,
    readonly onClose: () => void
}> = memo(props => {
    const zh = useZh();
    return zh? 
        <ViewDialog title='性能报告' {...props}>
            {ZH}
        </ViewDialog> : 
        <ViewDialog title='Performance Report' {...props}>
            {EN}
        </ViewDialog>;
});

export const PerformancePanel: FC = memo(() => {
    const zh = useZh();
    return zh ? ZH : EN;
});

const ZH =
    <>
        <Benchmark type='OPS' locale='zh'/>
        <b>每秒操作次数</b>
        <ul>
            <li>
                <b>横坐标</b>: 每次从数据库中查询到的数据对象的数量。
            </li>
            <li>
                <b>纵坐标</b>: 每秒操作次数。
            </li>
        </ul>
        <p>
            你也可以点击图标上方的<code>显示原生JDBC坐标</code>来和原始的JDBC对比。这样操作后，你会看到难以置信的结果并难免质疑其真实性，在性能指标相关的文档中，我们会对其会给予解释。
        </p>
    </>;

const EN = 
    <>
        <Benchmark type='OPS'/>
        <b>Operations per second</b>
        <ul>
            <li>
                <b>Abscissa</b>: The number of data objects queried from the database each time.
            </li>
            <li>
                <b>Vertical axis</b>: Operations per second.
            </li>
        </ul>
        <p>
            You can also click <code>Show native JDBC coordinates</code> above the icon to compare with the original JDBC. After doing this, you will see incredible results and inevitably question its authenticity. In the documentation related to performance indicators, we will explain it.
        </p>
    </>;
