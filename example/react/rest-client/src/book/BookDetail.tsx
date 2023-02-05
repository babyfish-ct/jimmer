import { Alert, CircularProgress, Grid, TextField } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { FC, memo, useMemo } from "react";
import { useRoute } from "wouter";
import { api } from "../common/ApiInstance";

export const BookDetail: FC = memo(() => {

    const [, params] = useRoute("/book/:id");
    
    const id = useMemo<number | undefined>(() => {
        const v = params !== null ? params?.id : undefined;
        if (typeof v === "string") {
            const n = parseInt(v);
            if (!isNaN(n)) {
                return n;
            }
        }
        return undefined;
    }, [params]);

    const { data, isLoading, error } = useQuery({
        queryKey: ["bookDetail", id],
        queryFn: () => api.bookService.findComplexBook({id: id!}),
        enabled: id !== undefined
    });

    if (isLoading) {
        return <CircularProgress/>;
    }

    if (error) {
        <Alert severity="error">Failed to load the book information</Alert>
    }

    return (
        <>
            <h1>Base information</h1>
            <Grid container>
                <Grid item>
                    <TextField/>
                </Grid>
            </Grid>
        </>
    );
});