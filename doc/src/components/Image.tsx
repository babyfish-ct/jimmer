import React, { FC, memo, useCallback, useMemo, useState } from "react";
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import { useZh } from "../util/use-zh";
import Grid from "@mui/material/Grid";
import BrowserOnly from '@docusaurus/BrowserOnly';

const Image: FC<{
    readonly src: string
}> = memo(({src}) => {
    return (
        <BrowserOnly>
            {() => <ImageCore src={src}/>}
        </BrowserOnly>
    );
});

const ImageCore: FC<{
    readonly src: string
}> = memo(({src}) => {
    const zh = useZh();
    const [loaded, setLoaded] = useState(false);
    const onLoad = useCallback(() => setLoaded(true), []);
    if (!isWebpSupported() && src.endsWith(".webp")) {
        return (
            <Alert security="error">
                {
                    zh ? 
                    "无法展示图片，请使用支持webp的浏览器" : 
                    "Unable to display image, please use a browser that supports webp"
                }
            </Alert>
        );
    }
    return (
        <>
            {
                !loaded &&
                <Grid container direction="column" alignItems="center">
                    <Grid item>
                        <CircularProgress/>
                    </Grid>
                    <Grid item>
                        {zh ? "加载图片中" : "Loading image"}
                    </Grid>
                </Grid>
            }
            <img src={src} onLoad={onLoad}/>
        </>
    );
});

export const DtoExplosion: FC = memo(() => {
    return <Image src={Src_DtoExplosion}/>;
});

export const ObjectCache: FC = memo(() => {
    return <Image src={Src_ObjectCache}/>;
});

export const AssociationCache: FC = memo(() => {
    return <Image src={Src_AssociationCache}/>;
});

export const CalculatedCache: FC = memo(() => {
    return <Image src={Src_CalculatedCache}/>;
});

export const MultiViewCache: FC = memo(() => {
    return <Image src={Src_MultiViewCache}/>;
});

export const Consistency: FC = memo(() => {
    return <Image src={Src_Consistency}/>;
});

export const Cloud: FC = memo(() => {
    return <Image src={Src_Cloud}/>;
});

export const GeneratedJava: FC = memo(() => {
    return <Image src={Src_GeneratedJava}/>;
});

export const GeneratedKt: FC = memo(() => {
    return <Image src={Src_GeneratedKt}/>;
});

export const Generated: FC = memo(() => {
    return <Image src={Src_Generated}/>;
});

export const SwaggerAuthorize: FC = memo(() => {
    return <Image src={Src_SwaggerAuthorize}/>;
});

export const GraphiqlHeaders: FC = memo(() => {
    return <Image src={Src_GraphiqlHeaders}/>;
});

export const Save: FC = memo(() => {
    return <Image src={Src_Save}/>;
});

export const Shape: FC = memo(() => {
    return <Image src={Src_Shape}/>;
});

export const Uml: FC = memo(() => {
    return <Image src={Src_Uml}/>;
});

export const VsApi: FC = memo(() => {
    return <Image src={Src_VsApi}/>;
});

export const VsCode: FC = memo(() => {
    return <Image src={Src_VsCode}/>;
});

export const VsFamily: FC = memo(() => {
    return <Image src={Src_VsFamily}/>;
});

export const VsField: FC = memo(() => {
    return <Image src={Src_VsField}/>;
});

export const VsModule: FC = memo(() => {
    return <Image src={Src_VsModule}/>;
});

export const PublicWechat: FC = memo(() => {
    return <Image src={Src_PublicWechat}/>;
});

let _isWebpSupported: boolean | undefined = undefined;

function isWebpSupported(): boolean {
    const value = _isWebpSupported;
    if (typeof value === "boolean") {
        return value;
    }
    var tmpCanvas = document.createElement('canvas');
    const newValue = tmpCanvas.getContext && 
        tmpCanvas.getContext("2d") &&
        tmpCanvas.toDataURL('image/webp').indexOf('data:image/webp') == 0;
    _isWebpSupported = newValue;
    return newValue;
}


const Src_DtoExplosion = require("@site/static/img/dto-explosion.webp").default;
const Src_ObjectCache = require("@site/static/img/object-cache.webp").default;
const Src_AssociationCache = require("@site/static/img/association-cache.webp").default;
const Src_CalculatedCache = require("@site/static/img/calculated-cache.webp").default;
const Src_MultiViewCache = require("@site/static/img/multi-view-cache.webp").default;
const Src_Consistency = require("@site/static/img/consistency.webp").default;
const Src_Cloud = require("@site/static/img/cloud.webp").default;
const Src_GeneratedJava = require("@site/static/img/generated-java.webp").default;
const Src_GeneratedKt = require("@site/static/img/generated-kt.webp").default;
const Src_Generated = require("@site/static/img/generated.webp").default;
const Src_SwaggerAuthorize = require("@site/static/img/swagger-authorize.webp").default;
const Src_GraphiqlHeaders = require("@site/static/img/graphiql-headers.webp").default;
const Src_Save = require("@site/static/img/save.webp").default;
const Src_Shape = require("@site/static/img/shape.webp").default;
const Src_Uml = require("@site/static/img/uml.svg").default;
const Src_VsApi = require("@site/static/img/vs-code/api.webp").default;
const Src_VsCode = require("@site/static/img/vs-code/code.webp").default;
const Src_VsFamily = require("@site/static/img/vs-code/family.webp").default;
const Src_VsField = require("@site/static/img/vs-code/field.webp").default;
const Src_VsModule = require("@site/static/img/vs-code/module.webp").default;
const Src_PublicWechat = require("@site/static/img/public-wechat.webp").default;