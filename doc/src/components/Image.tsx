import React, { FC, memo, useCallback, useState } from "react";
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import { useZh } from "../util/use-zh";
import Grid from "@mui/material/Grid";
import useIsBrowser from "@docusaurus/useIsBrowser";

const Image: FC<{
    readonly src: string
}> = memo(({src}) => {
    const zh = useZh();
    const [loaded, setLoaded] = useState(false);
    const isBrowser = useIsBrowser();
    const onLoad = useCallback(() => setLoaded(true), []);
    if (isBrowser && !isWebpSupported() && src.endsWith(".webp")) {
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
                isBrowser && !loaded &&
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
    return <Image src={require("@site/static/img/dto-explosion.webp").default}/>;
});

export const ObjectCache: FC = memo(() => {
    return <Image src={require("@site/static/img/object-cache.webp").default}/>;
});

export const AssociationCache: FC = memo(() => {
    return <Image src={require("@site/static/img/association-cache.webp").default}/>;
});

export const CalculatedCache: FC = memo(() => {
    return <Image src={require("@site/static/img/calculated-cache.webp").default}/>;
});

export const MultiViewCache: FC = memo(() => {
    return <Image src={require("@site/static/img/multi-view-cache.webp").default}/>;
});

export const Consistency: FC = memo(() => {
    return <Image src={require("@site/static/img/consistency.webp").default}/>;
});

export const Cloud: FC = memo(() => {
    return <Image src={require("@site/static/img/cloud.webp").default}/>;
});

export const GeneratedJava: FC = memo(() => {
    return <Image src={require("@site/static/img/generated-java.webp").default}/>;
});

export const GeneratedKt: FC = memo(() => {
    return <Image src={require("@site/static/img/generated-kt.webp").default}/>;
});

export const Generated: FC = memo(() => {
    return <Image src={require("@site/static/img/generated.webp").default}/>;
});

export const SwaggerAuthorize: FC = memo(() => {
    return <Image src={require("@site/static/img/swagger-authorize.webp").default}/>;
});

export const GraphiqlHeaders: FC = memo(() => {
    return <Image src={require("@site/static/img/graphiql-headers.webp").default}/>;
});

export const Save: FC = memo(() => {
    return <Image src={require("@site/static/img/save.webp").default}/>;
});

export const Shape: FC = memo(() => {
    return <Image src={require("@site/static/img/shape.webp").default}/>;
});

export const Uml: FC = memo(() => {
    return <Image src={require("@site/static/img/uml.svg").default}/>;
});

export const VsApi: FC = memo(() => {
    return <Image src={require("@site/static/img/vs-code/api.webp").default}/>;
});

export const VsCode: FC = memo(() => {
    return <Image src={require("@site/static/img/vs-code/code.webp").default}/>;
});

export const VsFamily: FC = memo(() => {
    return <Image src={require("@site/static/img/vs-code/family.webp").default}/>;
});

export const VsField: FC = memo(() => {
    return <Image src={require("@site/static/img/vs-code/field.webp").default}/>;
});

export const VsModule: FC = memo(() => {
    return <Image src={require("@site/static/img/vs-code/module.webp").default}/>;
});

export const PublicWechat: FC = memo(() => {
    return <Image src={require("@site/static/img/public-wechat.webp").default}/>;
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
