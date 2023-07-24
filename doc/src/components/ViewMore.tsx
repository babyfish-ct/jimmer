import { Button } from "@mui/material";
import React, { FC, memo, PropsWithChildren, useCallback, useState } from "react";
import { ViewDialog } from "./ViewDialog";

export const ViewMore: FC<
    PropsWithChildren<{
        readonly buttonText: string,
        readonly fullScreen?: boolean,
        readonly title?: string,
        readonly variant?: 'text' | 'outlined' | 'contained'
    }>
> = memo(({buttonText, fullScreen = false, title = buttonText, variant = "contained", children}) => {
    
    const [open, setOpen] = useState(false);

    const onButtonClick = useCallback(() => {
        setOpen(true);
    }, []);
    
    const onClose = useCallback(() => {
        setOpen(false);
    }, []);

    return (
        <>
            <Button onClick={onButtonClick} variant={variant} size={variant == 'outlined' ? "small" : "medium"}>{buttonText}</Button>
            <ViewDialog open={open} onClose={onClose} title={title} fullScreen={fullScreen}>
                {children}
            </ViewDialog>
        </>
    );
});

