import React, { FC, memo, ReactNode, useMemo } from "react";
import Grid from '@mui/material/Grid';
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardActions from "@mui/material/CardActions";
import Typography from "@mui/material/Typography";
import { ViewMore } from "./ViewMore";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";

export const Feedback: FC<{
    readonly items: ReadonlyArray<FeedbackItem>
}> = memo(({items}) => {

    const {i18n} = useDocusaurusContext();

    const isZh = useMemo(() => {
        const locale = i18n.currentLocale;
        return locale === "zh" || locale === "zh_CN" || locale === "zh_cn";
    }, [i18n.currentLocale]);

    return (
        <Grid container spacing={2} alignItems="stretch">
            {
                items.map(item => (       
                    <Grid key={item.author} container direction="column" item xs={12} lg={6}>
                        <Card elevation={3} style={{height: '100%'}}>
                            <CardContent>
                                <Grid item container>
                                    <Grid item xs={3}><Typography variant="h5" component="div">{item.author}</Typography></Grid>
                                    <Grid item xs={9} style={{textAlign: "right"}}><Typography variant="h6" component="div">{item.company}</Typography></Grid>
                                </Grid>
                            </CardContent>
                            <CardContent>
                                {item.content}
                            </CardContent>
                            {
                                item.detail &&
                                <CardActions>
                                    <ViewMore 
                                    buttonText={isZh ? "查看更多" : "More Details"} 
                                    title={isZh ? `来自${item.author}的反馈` : `Feedback from ${item.author}`}>
                                        {item.detail}
                                    </ViewMore>
                                </CardActions>
                            }
                        </Card>
                    </Grid> 
                ))
            }
        </Grid>
    )
});

export interface FeedbackItem {
    readonly author: string,
    readonly company: string,
    readonly content: ReactNode,
    readonly detail?: ReactNode
};

