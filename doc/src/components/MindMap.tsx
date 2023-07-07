import React, { CSSProperties, FC, memo, useCallback, useEffect, useMemo, useRef, useState } from "react";
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useBaseUrl from '@docusaurus/useBaseUrl';
import { Markmap } from "markmap-view";
import { Transformer } from 'markmap-lib';
import 'markmap-toolbar/dist/style.css';
import ButtonGroup from '@mui/material/ButtonGroup';
import IconButton from '@mui/material/IconButton';
import ZoomInIcon from '@mui/icons-material/ZoomIn';
import ZoomOutIcon from '@mui/icons-material/ZoomOut';
import FitScreenIcon from '@mui/icons-material/FitScreen';
import OpenInFullIcon from '@mui/icons-material/OpenInFull';
import CloseFullscreenIcon from '@mui/icons-material/CloseFullscreen';
import { AppBar, Button, Dialog, DialogContent, Toolbar, Typography } from "@mui/material";

export const MindMap: FC<{
    readonly initialExpandLevel?: number
}> = memo(({initialExpandLevel = 1}) => {

    const [maximized, setMaximized] = useState(false);

    const onMaximize = useCallback(() => {
        setMaximized(true);
    }, []);

    const onRestore = useCallback(() => {
        setMaximized(false);
    }, []);

    return (
        <>
            <Dialog 
            open={maximized} 
            onClose={onRestore} 
            fullScreen={true}>
                <AppBar sx={{ position: 'relative' }}>
                    <Toolbar>
                        <Typography sx={{ ml: 2, flex: 1 }} variant="h6" component="div">
                            Jimer Mind Map
                        </Typography>
                        <IconButton aria-label="close" onClick={onRestore} style={{color:'white'}}>
                            <CloseFullscreenIcon/>
                        </IconButton>
                    </Toolbar>
                </AppBar>
                <DialogContent>
                <MindMapCore/>
                </DialogContent>
            </Dialog>
            <MindMapCore initialExpandLevel={initialExpandLevel} onMaximize={onMaximize}/>
        </>
    );
});

const MindMapCore: FC<{
    readonly initialExpandLevel?: number,
    readonly onMaximize?: () => void
}> = memo(({initialExpandLevel, onMaximize}) => {

    const { i18n } = useDocusaurusContext();

    const baseUrl = useBaseUrl('docs/team-work');

    const data = useMemo(() => {
        const locale = i18n.currentLocale;
        const rawText = locale === 'zh' || locale === 'zh_CN' || locale === 'zh_cn' ? 
            DATA_ZH : 
            DATA_EN;
        return rawText.split('@site').join(baseUrl);  
    }, [i18n.currentLocale, baseUrl]);

    const refSvg = useRef<SVGSVGElement>();

    const refMm = useRef<Markmap>();

    useEffect(() => {
        // Create markmap and save to refMm
        const mm = Markmap.create(refSvg.current, {
            initialExpandLevel
        });
        refMm.current = mm;
        return () => {
            mm.destroy();
        }
    }, [refSvg.current]);

    useEffect(() => {
        // Update data for markmap once value is changed
        const mm = refMm.current;
        if (!mm) return;
        const { root } = TRANSFORMER.transform(data);
        mm.setData(root);
        mm.fit();
    }, [refMm.current, data]);

    const onZoomInClick = useCallback(() => {
        refMm.current.rescale(1.25);
    }, [refMm.current]);

    const onZoomOutClick = useCallback(() => {
        refMm.current.rescale(0.8);
    }, [refMm.current]);

    const onFitScreenClick = useCallback(() => {
        refMm.current.fit();
    }, [refMm.current]);

    const onMaximizeClick = useCallback(() => {
        if (onMaximize !== undefined) {
            onMaximize();
        }
    }, [onMaximize]);

    return (
        <div style={{position: 'relative', width:'100%', paddingTop: '56.25%'}}>
            <div>
                <svg style={{position: 'absolute', left: 0, top: 0, width: '100%', height: '100%'}} ref={refSvg} />
            </div>
            <div style={{position: 'absolute', right: 0, top: 0}}>
                <ButtonGroup variant="contained" aria-label="outlined primary button group" style={{backgroundColor: "white"}}>
                    <IconButton aria-label="zoonIn" onClick={onZoomInClick}>
                        <ZoomInIcon/>
                    </IconButton>
                    <IconButton aria-label="zoonOut" onClick={onZoomOutClick}>
                        <ZoomOutIcon/>
                    </IconButton>
                    <IconButton aria-label="fit" onClick={onFitScreenClick}>
                        <FitScreenIcon/>
                    </IconButton>
                    {
                        initialExpandLevel !== undefined &&
                        <IconButton aria-label="maximize" onClick={onMaximizeClick}>
                            <OpenInFullIcon/>
                        </IconButton>
                    }
                </ButtonGroup>
            </div>
        </div>
    );
});

const TRANSFORMER = new Transformer();

const DATA_ZH = `
-   Jimmer

    -   [询任意数据结构](@site/query/object-fetcher)

        -   GraphQL HTTP服务的能力被拓展成无处不在本地代码行为

        -   精确控制每个属性是否需要查询

        -   [可对自关联属性进行递归查询](@site/query/object-fetcher/recursive)

        -   统一不同数据关联，对开发者透明

            -   数据库中的关联

            -   自定义计算属性

            -   跨越微服务的远程关联
            
            -   优先读取缓存

    -   [保存任意数据结构](@site/mutation/save-command)

        -   快速开发任意复杂的表单保存业务

        -   对同一个聚合根类型而言，多种不同表单格式的保存业务可并存

    -   [缓存及其一致性](@site/cache)

        -   丰富底层缓存类型，合力为用户抽象任意数据结构缓存

            -   [对象缓存](@site/cache/cache-type/object)

            -   [关联缓存](@site/cache/cache-type/association)

            -   [计算缓存](@site/cache/cache-type/calculation)

        -   自由控制各数据是否启用缓存，精确到属性

        -   透明性。各数据是否启用缓存对代码无影响。缓存逻辑破坏代码简洁性的痛苦一去不复返

        -   多级缓存支持，采用多少级，每一级采用什么缓存技术由用户决定

        -   [多视图缓存，不同用户看到不同的缓存](@site/cache/multiview-cache)

        -   强大的一致性。其中，对象缓存和关联缓存的一致性是全自动的，无需任何开发

    -   [计算属性](../mapping/advanced/calculated/transient)

        -   计算逻辑和ORM解耦，可以利用业务信息(如身份权限)计算，允许使用非SQL技术

        -   不仅可以是标量属性，还可以是关联属性

    -   [远程关联](@site/spring/spring-cloud)

        -   跨越微服务的数据关联

        -   对微服务治理框架不做假设

        -   将不同微服务的关系型模型，合并成一个全局的关系模型

    -   强类型SQL DSL

        -   [可混入NativeSQL片段，不再惧怕通用DSL无法使用特定数据库特的非通用功能](@site/query/native-sql)

        -   [Jimmer独创的隐式动态table join，填补行业空白](@site/query/dynamic-join/)

        -   编译时发现问题，不再惧怕重构

        -   良好的IDE智能提示，流畅开发

    -   [前后端免对接](@site/spring/client)

        -   消除后端开发的DTO爆炸，从以传统开发方式(尤其是MyBatis)的重复劳动中解脱出来

        -   为前端自动生成TypeScript代码，在前端代码中恢复DTO类型定义，让每个业务接口都有精确的返回类型定义

        -   把后端异常映射为前端可理解错误信息

    -   [全局过滤器](@site/query/global-filter)

        -   为实体类添加过滤条件，自动应用于绝大部分查询

        -   内置软删除过滤器支持

        -   [多视角缓存，过滤器导致不同的用户看到不同数据，相应地，不同的用户可看到不同的缓存](@site/cache/multiview-cache)

    -   [智能分页](@site/query/paging)

        -   用户编写data询，自动生成count查询，并自动优化去掉非必要table join

        -   当页码过大时，自动变换查询方式

    -   [极致性能](@site/overview/benchmark)

        -   ORM本身的映射性能极高，发挥虚拟线程的威力

        -   不会如传统ORM一样被新手诱导出低性能SQL
`;

const DATA_EN = `
-   Jimmer

    -   [Query any data structure](@site/query/object-fetcher)

        -   Extend the capabilities of GraphQL HTTP service to behave like local code

        -   Precisely control whether each property needs to be queried

        -   [Recursive query for self-associated properties](@site/query/object-fetcher/recursive)

        -   Unified handling of different data associations, transparent to developers

            -   Associations in the database

            -   Custom calculated properties

            -   Remote associations across microservices

            -   Prioritize reading from cache

    -   [Save any data structure](@site/mutation/save-command)

        -   Rapidly develop complex form-saving business logic

        -   Multiple different form formats for saving business logic can coexist for the same aggregate root type

    -   [Caching and its consistency](@site/cache)

        -   Rich variety of underlying cache types, collectively abstracting caching for any data structure

            -   [Object cache](@site/cache/cache-type/object)

            -   [Association cache](@site/cache/cache-type/association)

            -   [Calculation cache](@site/cache/cache-type/calculation)

        -   Precisely control whether each property needs to be cached

        -   Transparency. Enabling caching for data has no impact on the code. The pain of cache logic compromising code simplicity is eliminated

        -   Support for multi-level caching, with the number of levels and the caching technology for each level determined by the user

        -   [Multi-view caching, where different users see different caches](@site/cache/multiview-cache)

        -   Powerful consistency. Consistency for object cache and association cache is fully automatic, requiring no development effort

    -   [Calculated properties](../mapping/advanced/calculated/transient)

        -   Decoupling of computation logic and ORM, allowing calculations based on business information (such as identity and permissions) and the use of non-SQL technologies

        -   Can be scalar properties as well as association properties

    -   [Remote associations](@site/spring/spring-cloud)

        -   Data associations across microservices

        -   No assumptions about the microservice governance framework

        -   Merge relational models from different microservices into a global relational model

    -   Strongly-typed SQL DSL

        -   [NativeSQL fragments can be mixed, no longer fear generic DSLs lacking non-generic functionality specific to certain databases](@site/query/native-sql)

        -   [Jimmer's innovative implicit dynamic table join, filling an industry gap](@site/query/dynamic-join/)

        -   Discover issues at compile time, no longer fear refactoring

        -   Excellent IDE smart suggestions for smooth development

    -   [No need for frontend-backend integration](@site/spring/client)

        -   Eliminate DTO explosion for backend development, freeing from repetitive work using traditional development approaches (especially MyBatis)

        -   Automatically generate TypeScript code for the frontend, restoring DTO type definitions in the frontend code, ensuring precise return type definitions for each business interface

        -   Map backend exceptions to frontend-understandable error messages

    -   [Global filters](@site/query/global-filter)

        -   Add filtering conditions to entity classes, automatically applied to the majority of queries

        -   Built-in support for soft delete filters

        -   [Multi-view caching, where filters result in different data being seen by different users, accordingly, different users can see different caches](@site/cache/multiview-cache)

    -   [Intelligent pagination](@site/query/paging)

        -   User writes data query, automatically generates count query, and optimizes unnecessary table joins

        -   Automatically switch query methods when the page number is too large

    -   [Ultimate performance](@site/overview/benchmark)

        -   ORM mapping performance is extremely high, leveraging the power of virtual threads

        -   Will not suffer from low-performance SQL induced by beginners like traditional ORMs
`;