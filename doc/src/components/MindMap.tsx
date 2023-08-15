import React, { FC, createElement, memo, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import useBaseUrl from '@docusaurus/useBaseUrl';
import { Markmap } from "markmap-view";
import { Transformer } from 'markmap-lib';
import 'markmap-toolbar/dist/style.css';
import IconButton from '@mui/material/IconButton';
import ZoomInIcon from '@mui/icons-material/ZoomIn';
import ZoomOutIcon from '@mui/icons-material/ZoomOut';
import FitScreenIcon from '@mui/icons-material/FitScreen';
import OpenInFullIcon from '@mui/icons-material/OpenInFull';
import CloseFullscreenIcon from '@mui/icons-material/CloseFullscreen';
import AppBar from "@mui/material/AppBar";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import Grid from "@mui/material/Grid";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { useZh } from "../util/use-zh";
import { ObjectFetcherDialog } from "./HomepageFeatures/ObjectFetcher";
import { SaveCommandDialog } from "./HomepageFeatures/SaveCommand";
import { ViewDialog } from "./ViewDialog";
import { CacheConsistencyDialog } from "./HomepageFeatures/CacheConsistency";
import { DynamicJoinProblemDialog } from "./HomepageFeatures/DynamicJoinProblem";
import { PerformanceDialog } from "./HomepageFeatures/Performance";
import { CommunicationDialog } from "./HomepageFeatures/Communication";
import { LongAssociation } from "./LongAssociation";
import { ObjectCache, AssociationCache, CalculatedCache, MultiViewCache } from "@site/src/components/Image";

export const MindMap: FC = memo(() => {

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
                            Jimmer Mind Map
                        </Typography>
                        <IconButton aria-label="close" onClick={onRestore} style={{color:'white'}}>
                            <CloseFullscreenIcon/>
                        </IconButton>
                    </Toolbar>
                </AppBar>
                <DialogContent>
                    <MindMapCore maximize={true}/>
                </DialogContent>
            </Dialog>
            <MindMapCore maximize={false} onMaximize={onMaximize}/>
        </>
    );
});

const MindMapCore: FC<{
    readonly maximize: boolean,
    readonly onMaximize?: () => void
}> = memo(({maximize, onMaximize}) => {

    const isZh = useZh();

    const baseUrl = useBaseUrl('docs');

    const [objectFetcherOpen, setObjectFetcherOpen] = useState(false);
    const [saveCommandOpen, setSaveCommandOpen] = useState(false);
    const [longAssociationOpen, setLongAssocaitionOpen] = useState(false);
    const [objectCacheOpen, setObjectCacheOpen] = useState(false);
    const [associationCacheOpen, setAssociationCacheOpen] = useState(false);
    const [calculatedCacheOpen, setCalculatedCacheOpen] = useState(false);
    const [multiViewCacheOpen, setMultiViewCacheOpen] = useState(false);
    const [cacheConsistencyOpen, setCacheConsistencyOpen] = useState(false);
    const [communicationOpen, setCommunicationOpen] = useState(false);
    const [dynamicJoinProlemOpen, setDynamicJoinProblemOpen] = useState(false);
    const [performanceOpen, setPerformanceOpen] = useState(false);
    
    const onViewMoreClick = useCallback((viewName: string) => {
        switch (viewName) {
            case "object-fetcher":
                setObjectFetcherOpen(true);
                break;
            case "save-command":
                setSaveCommandOpen(true);
                break;
            case "long-association":
                setLongAssocaitionOpen(true);
                break;
            case "object-cache":
                setObjectCacheOpen(true);
                break;
            case "association-cache":
                setAssociationCacheOpen(true);
                break;
            case "calculated-cache":
                setCalculatedCacheOpen(true);
                break;
            case "multi-view-cache":
                setMultiViewCacheOpen(true);
                break;
            case "cache-consistency":
                setCacheConsistencyOpen(true);
                break;
            case "communication":
                setCommunicationOpen(true);
                break;
            case "dynamic-join-problem":
                setDynamicJoinProblemOpen(true);
                break;
            case "performance":
                setPerformanceOpen(true);
                break;
            default:
                console.warn("Illegal view name: " + viewName);
                break;
        }
    }, []);

    const onObjectFetcherClose = useCallback(() => {
        setObjectFetcherOpen(false);
    }, []);

    const onSaveCommandClose = useCallback(() => {
        setSaveCommandOpen(false);
    }, []);

    const onLongAssociationClose = useCallback(() => {
        setLongAssocaitionOpen(false);
    }, []);

    const onObjectCacheClose = useCallback(() => {
        setObjectCacheOpen(false);
    }, []);

    const onAssocitionCacheClose = useCallback(() => {
        setAssociationCacheOpen(false);
    }, []);

    const onCalculatedCacheClose = useCallback(() => {
        setCalculatedCacheOpen(false);
    }, []);

    const onMultiViewCacheClose = useCallback(() => {
        setMultiViewCacheOpen(false);
    }, []);

    const onCacheConsistencyClose = useCallback(() => {
        setCacheConsistencyOpen(false);
    }, []);

    const onCommunicationClose = useCallback(() => {
        setCommunicationOpen(false);
    }, []);

    const onDynamicJoinProblemClose = useCallback(() => {
        setDynamicJoinProblemOpen(false);
    }, []);

    const onPerformanceClose = useCallback(() => {
        setPerformanceOpen(false);
    }, []);

    const onMaximizeClick = useCallback(() => {
        if (onMaximize !== undefined) {
            onMaximize();
        }
    }, [onMaximize]);

    const [cardType, setCardType] = useState(0);
    const onCardTypeChange = useCallback((event: React.SyntheticEvent, newValue: number) => {
        setCardType(newValue);
    }, []);
    return (
        <div>
            {
                !maximize &&
                <Tabs value={cardType} onChange={onCardTypeChange}>
                    <Tab value={0} label={isZh ? "脑图" : "Mindmap"}/>
                    <Tab value={1} label={isZh ? "文本" : "Text"}/>
                </Tabs>
            }
            {
                cardType === 0 &&
                <MindMapGraphis text={isZh ? DATA_ZH : DATA_EN} onView={onViewMoreClick} baseUrl={baseUrl} maximize={maximize} onMaximizeClick={onMaximizeClick}/>
            }
            {
                cardType === 1 &&
                <MindMapText text={isZh ? DATA_ZH : DATA_EN} onView={onViewMoreClick} baseUrl={baseUrl}/>
            }
            <ObjectFetcherDialog open={objectFetcherOpen} onClose={onObjectFetcherClose}/>
            <SaveCommandDialog open={saveCommandOpen} onClose={onSaveCommandClose}/>
            <ViewDialog title={isZh ? "复杂的表单保存业务" : "Complex form saving business"} open={longAssociationOpen} onClose={onLongAssociationClose}>
                <LongAssociation/>
            </ViewDialog>
            <ViewDialog title={isZh ? "对象缓存" : "Object Cache"} open={objectCacheOpen} onClose={onObjectCacheClose}>
                <ObjectCache/>
            </ViewDialog>
            <ViewDialog title={isZh ? "关联缓存" : "Association Cache"} open={associationCacheOpen} onClose={onAssocitionCacheClose}>
                <AssociationCache/>
            </ViewDialog>
            <ViewDialog title={isZh ? "计算缓存" : "Association Cache"} open={calculatedCacheOpen} onClose={onCalculatedCacheClose}>
                <CalculatedCache/>
            </ViewDialog>
            <ViewDialog title={isZh ? "多视图缓存" : "Multi-view Cache"} open={multiViewCacheOpen} onClose={onMultiViewCacheClose}>
                <MultiViewCache/>
            </ViewDialog>
            <CacheConsistencyDialog open={cacheConsistencyOpen} onClose={onCacheConsistencyClose}/>
            <CommunicationDialog open={communicationOpen} onClose={onCommunicationClose}/>
            <DynamicJoinProblemDialog open={dynamicJoinProlemOpen} onClose={onDynamicJoinProblemClose}/>
            <PerformanceDialog open={performanceOpen} onClose={onPerformanceClose}/>
        </div>
    );
});

const DATA_ZH = `
-   Jimmer

    -   查询任意形状的数据结构 [👁](#object-fetcher) [→](@site/query/object-fetcher)

        -   GraphQL HTTP服务的能力被拓展成无处不在本地代码行为

        -   精确控制每个属性是否需要查询

        -   可对自关联属性进行递归查询 [→](@site/query/object-fetcher/recursive)

        -   统一不同数据关联，对开发者透明

            -   数据库中的关联 [→](@site/mapping/base/association/)

            -   自定义计算属性 [→](@site/mapping/advanced/calculated/transient#关联计算bookstorenewestbooks)

            -   跨越微服务的远程关联 [→](@site/spring/spring-cloud)
            
            -   优先读取缓存

    -   保存任意数据结构 [👁](#save-command) [→](@site/mutation/save-command)

        -   快速开发任意复杂的表单保存业务 [👁](#long-association)

        -   对同一个聚合根类型而言，多种不同表单格式的保存业务可并存

    -   缓存及其一致性 [→](@site/cache)

        -   丰富底层缓存类型，上层抽象任意数据结构

            -   对象缓存 [👁](#object-cache) [→](@site/cache/cache-type/object)

            -   关联缓存 [👁](#association-cache) [→](@site/cache/cache-type/association)

            -   计算缓存 [👁](#calculated-cache) [→](@site/cache/cache-type/calculation)

        -   自由控制各数据是否启用缓存，精确到属性

        -   透明性。各数据是否启用缓存对代码无影响。缓存逻辑破坏代码简洁性的痛苦一去不复返

        -   多级缓存支持，采用多少级，每一级采用什么缓存技术由用户决定

        -   多视图缓存，不同用户看到不同的缓存 [👁](#multi-view-cache) [→](@site/cache/multiview-cache)

        -   强大的一致性。其中，对象缓存和关联缓存的一致性是全自动的，无需任何开发 [👁](#cache-consistency) [→](@site/cache/consistency)

    -   计算属性 [→](@site/mapping/advanced/calculated/)
    
        -   计算逻辑和ORM解耦，可以利用业务信息(如身份权限)计算，允许使用非SQL技术

        -   不仅可以是标量属性，还可以是关联属性

    -   远程关联 [→](@site/spring/spring-cloud)

        -   跨越微服务的数据关联

        -   对微服务治理框架不做假设

        -   将不同微服务的关系型模型，合并成一个全局的关系模型

    -   强类型SQL DSL

        -   Jimmer独创的隐式动态table join，填补行业空白 [👁](#dynamic-join-problem) [→](@site/query/dynamic-join)

        -   可混入NativeSQL片段，不再惧怕通用DSL无法使用特定数据库特的非通用功能 [→](@site/query/native-sql)

        -   编译时发现问题，不再惧怕重构

        -   良好的IDE智能提示，流畅开发

    -   前后端免对接 [👁](#communication) [→](@site/spring/client)

        -   消除后端开发的DTO爆炸，从以传统开发方式(尤其是MyBatis)的重复劳动中解脱出来

        -   为前端自动生成TypeScript代码，在前端代码中恢复DTO类型定义，让每个业务接口都有精确的返回类型定义

        -   把后端异常映射为前端可理解错误信息

    -   全局过滤器 [→](@site/query/global-filter)

        -   为实体类添加过滤条件，自动应用于绝大部分查询

        -   内置软删除过滤器支持 [→](@site/query/global-filter/logical-deleted)

        -   多视角缓存，过滤器导致不同的用户看到不同数据，相应地，不同的用户可看到不同的缓存 [👁](#multi-view-cache) [→](@site/cache/multiview-cache)

    -   智能分页 [→](@site/query/paging)

        -   用户编写data询，自动生成count查询，并自动优化去掉非必要table join [→](@site/query/paging/unncessary-join)

        -   如果分页查询期望的观察区域在分页前所有数据的后半部分，进行反排序优化 [→](@site/query/paging/reverse-sorting)

        -   当页码过大时，自动变换查询方式 [→](@site/query/paging/deep-optimization)

    -   DTO语言 [→](@site/object/view/dto-language)

        -   自动生成DTO类型的Java/Kotlin定义

        -   自动生成实体和DTO之间的相互转化逻辑

        -   自动生成查询DTO查询逻辑

    -   极致性能 [👁](#performance) [→](@site/overview/benchmark)

        -   ORM本身的映射性能极高，发挥虚拟线程的威力

        -   不会如传统ORM一样被新手诱导出低性能SQL
`;

const DATA_EN = `
-   Jimmer

    -   Query data structure of any shape[👁](#object-fetcher) [→](@site/query/object-fetcher)

        -   Extend the capabilities of GraphQL HTTP service to behave like local code

        -   Precisely control whether each property needs to be queried

        -   Recursive query for self-associated properties [→](@site/query/object-fetcher/recursive)

        -   Unified handling of different data associations, transparent to developers

            -   Associations in the database [→](@site/mapping/base/association/)

            -   Custom calculated properties [→](@site/mapping/advanced/calculated/transient#关联计算bookstorenewestbooks)

            -   Remote associations across microservices [→](@site/spring/spring-cloud)

            -   Prioritize reading from cache

    -   Save data structure of any shape[👁](#save-command) [→](@site/mutation/save-command)

        -   Rapidly develop complex form-saving business logic

        -   Multiple different form formats for saving business logic can coexist for the same aggregate root type

    -   Caching and its consistency [→](@site/cache)

        -   Rich variety of underlying cache types, collectively abstracting caching for any data structure

            -   Object cache [👁](#object-cache) [→](@site/cache/cache-type/object)

            -   Association cache [👁](#association-cache) [→](@site/cache/cache-type/association)

            -   Calculated cache [👁](#calculated-cache) [→](@site/cache/cache-type/calculation)

        -   Precisely control whether each property needs to be cached

        -   Transparency. Enabling caching for data has no impact on the code. The pain of cache logic compromising code simplicity is eliminated

        -   Support for multi-level caching, with the number of levels and the caching technology for each level determined by the user

        -   Multi-view caching, where different users see different caches [👁](#multi-view-cache) [→](@site/cache/multiview-cache)

        -   Powerful consistency. Consistency for object cache and association cache is fully automatic, requiring no development effort [👁](#cache-consistency) [→](@site/cache/consistency)

    -   Calculated properties [→](@site/mapping/advanced/calculated/)

        -   Decoupling of computation logic and ORM, allowing calculations based on business information (such as identity and permissions) and the use of non-SQL technologies

        -   Can be scalar properties as well as association properties

    -   Remote associations [→](@site/spring/spring-cloud)

        -   Data associations across microservices

        -   No assumptions about the microservice governance framework

        -   Merge relational models from different microservices into a global relational model

    -   Strongly-typed SQL DSL

        -   Jimmer's innovative implicit dynamic table join, filling an industry gap [👁](#dynamic-join-problem) [→](@site/query/dynamic-join)

        -   NativeSQL fragments can be mixed, no longer fear generic DSLs lacking non-generic functionality specific to certain databases [→](@site/query/native-sql)

        -   Discover issues at compile time, no longer fear refactoring

        -   Excellent IDE smart suggestions for smooth development

    -   No need for frontend-backend integration [👁](#communication) [→](@site/spring/client)

        -   Eliminate DTO explosion for backend development, freeing from repetitive work using traditional development approaches (especially MyBatis)

        -   Automatically generate TypeScript code for the frontend, restoring DTO type definitions in the frontend code, ensuring precise return type definitions for each business interface

        -   Map backend exceptions to frontend-understandable error messages

    -   Global filters [→](@site/query/global-filter)

        -   Add filtering conditions to entity classes, automatically applied to the majority of queries

        -   Built-in support for soft delete filters

        -   Multi-view caching, where filters result in different data being seen by different users, accordingly, different users can see different caches [👁](#multi-view-cache) [→](@site/cache/multiview-cache)

    -   Intelligent pagination [→](@site/query/paging)

        -   User writes data query, automatically generates count query, and optimizes unnecessary table joins [→](@site/query/paging/unncessary-join)

        -   If the expected observation area of the pagination query is in the second half of all the data before paging, perform reverse sorting optimization [→](@site/query/paging/reverse-sorting)

        -   Automatically switch query methods when the page number is too large [→](@site/query/paging/deep-optimization)

    - DTO language [→](@site/object/view/dto-language)

        - Automatically generate Java/Kotlin definitions of DTO types
        
        - Automatically generate the transforming logic between entities and DTOs
        
        - Automatically generate query DTO query logic

    -   Ultimate performance [👁](#performance) [→](@site/overview/benchmark)

        -   ORM mapping performance is extremely high, leveraging the power of virtual threads

        -   Will not suffer from low-performance SQL induced by beginners like traditional ORMs
`;

const MindMapGraphis: FC<{
    readonly text: string,
    readonly onView: (viewName: string) => void,
    readonly baseUrl: string,
    readonly maximize: boolean,
    readonly onMaximizeClick: () => void
}> = memo(({text, onView, baseUrl, maximize, onMaximizeClick}) => {

    const isZh = useZh();

    const refDom = useRef<HTMLDivElement>();
    const refSvg = useRef<SVGSVGElement>();
    const refMm = useRef<Markmap>();

    const data = useMemo(() => {
        return text.split('@site').join(baseUrl);  
    }, [isZh, text, baseUrl]);

    useEffect(() => {
        if (refSvg.current === null) {
            return;
        }
        refSvg.current.addEventListener("wheel", (e) => e.preventDefault());
        // Create markmap and save to refMm
        const mm = Markmap.create(refSvg.current, {
            scrollForPan: false
        });
        refMm.current = mm;
        return () => {
            mm.destroy();
        }
    }, [refSvg]);

    useEffect(() => {
        // Update data for markmap once value is changed
        const mm = refMm.current;
        if (!mm) return;
        const { root } = new Transformer().transform(data);
        mm.setData(root);
        mm.fit();
    }, [refMm.current, data]);

    useEffect(() => {
        if (refDom.current === null) {
            return;
        }
        setTimeout(() => {
            const links = refDom.current.getElementsByTagName("A");
            for (let i = links.length - 1; i >= 0; --i) {
                const link: HTMLLinkElement = links.item(i) as HTMLLinkElement;
                const href = link.getAttribute("href");
                if (href.startsWith("#")) {
                    link.style.cursor = "pointer"
                    link.removeAttribute("href");
                    if (link.addEventListener) {
                        link.addEventListener("click", () => {
                            onView(href.substring(1));
                        });
                    } else { // Damn IE6 of XP
                        (link as any).attachEvent("onClick", () => {
                            onView(href.substring(1));
                        });
                    }
                }
            }
        }, 0);
    }, [refDom.current, onView]);

    const onFitScreenClick = useCallback(() => {
        refMm.current.fit();
    }, [refMm.current]);

    const onZoomInClick = useCallback(() => {
        refMm.current.rescale(1.25);
    }, [refMm.current]);

    const onZoomOutClick = useCallback(() => {
        refMm.current.rescale(0.8);
    }, [refMm.current]);

    return (
        <>
            <Grid container alignItems="center">
                <Grid item flex={1}></Grid>
                <Grid item>
                    {
                        !maximize &&
                        <Button  onClick={onMaximizeClick} size="small">
                            <OpenInFullIcon/>{isZh ? "最大化" : "Maximize"}
                        </Button>
                    }
                    <Button onClick={onFitScreenClick} size="small">
                        <FitScreenIcon/>{isZh ? "适应当前窗口" : "Fit current window"}
                    </Button>
                    <Button onClick={onZoomInClick} size="small">
                        <ZoomInIcon/>{isZh ? "放大" : "Zoom In"}
                    </Button>
                    <Button onClick={onZoomOutClick} size="small">
                        <ZoomOutIcon/>{isZh ? "缩小" : "Zoom Out"}
                    </Button>
                </Grid>
            </Grid>
            <div style={{position: 'relative', width:'100%', paddingTop: '56.25%'}}>
                <div ref={refDom}>
                    <svg style={{position: 'absolute', left: 0, top: 0, width: '100%', height: '100%'}} ref={refSvg}/>
                </div>
            </div>
        </>
    );
});

const MindMapText: FC<{
    readonly text: string,
    readonly onView: (viewName: string) => void,
    readonly baseUrl: string
}> = memo(({text, onView, baseUrl}) => {

    const refDiv = useRef<HTMLDivElement>();
    
    useEffect(() => {
        const div = refDiv.current;
        if (div === null) {
            return;
        }
        const builder = new TreeBuilder(onView, baseUrl);
        for (const line of text.split(/\n/)) {
            builder.append(line);
        }
        div.appendChild(builder.build());
    }, [refDiv.current, text]);
    return (
        <div ref={refDiv}></div>
    );
});

class TreeBuilder {

    private root: HTMLUListElement = document.createElement("UL") as HTMLUListElement;

    private list: HTMLUListElement = this.root;

    private item: HTMLLIElement | undefined = undefined;

    private depth = 1;

    constructor(
        private onView: (viewName: string) => void,
        private baseUrl: string
    ) {}

    append(line: string) {
        const index = line.indexOf('-');
        if (index <= 0) {
            return;
        }
        const prefix = line.substring(0, index);
        if (prefix.indexOf("\t") !== -1 || index % 4 != 0) {
            throw new Error(`Illegal input: "${line}"`);
        }
        const depth = prefix.length / 4;
        if (depth > this.depth) {
            for (let i = depth - this.depth; i > 0; --i) {
                if (this.item === undefined) {
                    this.item = document.createElement("LI") as HTMLLIElement;
                    this.list.appendChild(this.item);
                }
                const ul = document.createElement("UL") as HTMLUListElement;
                this.item.appendChild(ul);
                this.list = ul;
                this.item = undefined;
            }
            this.depth = depth;
        } else if (depth < this.depth) {
            for (let i = this.depth - depth; i > 0; --i) {
                this.list = this.list.parentNode.parentNode as HTMLUListElement;
            }
            this.item === undefined;
            this.depth = depth;
        }
        const li = document.createElement("LI") as HTMLLIElement;
        switch (depth) {
            case 1:
                li.style.fontSize = "1.2rem";
                li.style.fontWeight = "bold";
                break;
            case 2:
                li.style.fontSize = "1.1rem";
                li.style.fontWeight = "normal";
                break;
            default:
                li.style.fontSize = "1rem";
                li.style.fontWeight = "bold";
                break;
        }
        this.list.appendChild(li);
        this.item = li;
        this.appendCore(line.substring(index + 1));
    }

    private appendCore(line: string) {
        let base = 0;
        let first = -1;
        while (true) {
            const [start, end, text, href] = this.findLink(line, base);
            if (start === -1) {
                break;
            }
            if (first === -1) {
                first = start;
            }
            base = end + 1;
            this.item.appendChild(document.createTextNode(" "));
            this.item.appendChild(this.createLink(text, href));
        }
        if (first !== -1) {
            this.item.insertBefore(
                document.createTextNode(line.substring(0, first).trim()),
                this.item.firstChild,
            );
        } else {
            this.item.appendChild(document.createTextNode(line));
        }
    }

    private createLink(text: string, href: string) {
        const a = document.createElement("A");
        a.appendChild(document.createTextNode(text));
        if (href.startsWith("#")) {
            a.style.cursor = "pointer";
            if (a.addEventListener) {
                a.addEventListener("click", () => {
                    this.onView(href.substring(1));
                });
            } else { // Damn IE6 of XP
                (a as any).attachEvent("onClick", () => { 
                    this.onView(href.substring(1)) 
                });
            }
        } else {
            a.setAttribute("href", href.replace("@site", this.baseUrl));
        }
        return a;
    }

    private findLink(line: string, base: number): [number, number, string, string] {
        const index1 = line.indexOf('[', base);
        const index2 = line.indexOf(']', base);
        if (index1 === -1 || index2 === -1 || index1 > index2) {
            return [-1, -1, "", ""];
        }
        const text = line.substring(index1 + 1, index2);
        const index3 = line.indexOf('(', index2 + 1);
        const index4 = line.indexOf(')', index2 + 1);
        if (index3 === -1 || index4 === -1 || index3 > index4) {
            return [-1, -1, "", ""];
        }
        const href = line.substring(index3 + 1, index4);
        return [index1, index4, text, href];
    }

    build(): HTMLElement {
        return this.root;
    }
}
