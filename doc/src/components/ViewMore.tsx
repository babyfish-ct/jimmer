import { AppBar, Button, Dialog, DialogContent, Toolbar, Typography } from "@mui/material";
import React, { FC, memo, PropsWithChildren, useCallback, useState } from "react";
import Slide from '@mui/material/Slide';
import { TransitionProps } from '@mui/material/transitions';


export const ViewMore: FC<
    PropsWithChildren<{
        readonly buttonText: string,
        readonly fullScreen?: boolean,
        readonly title: string
    }>
> = memo(({buttonText, fullScreen = false, title, children}) => {
    
    const [open, setOpen] = useState(false);
    const [maximize, setMaximize] = useState(fullScreen);
    const onButtonClick = useCallback(() => {
        setOpen(true);
    }, []);
    const onClose = useCallback(() => {
        setOpen(false);
    }, []);
    const onResize = useCallback(() => {
        setMaximize(old => !old);
    }, []);

    return (
        <>
            <Button onClick={onButtonClick} variant="contained">{buttonText}</Button>
            <Dialog open={open} onClose={onClose} fullScreen={maximize} TransitionComponent={Transition}>
                <AppBar sx={{ position: 'relative' }}>
                    <Toolbar>
                        <Typography sx={{ ml: 2, flex: 1 }} variant="h6" component="div">
                            {title}
                        </Typography>
                        <Button autoFocus color="inherit" onClick={onResize}>
                            {maximize ? "Recover" : "Maximize"}                            
                        </Button>
                        <Button autoFocus color="inherit" onClick={onClose}>
                            Close
                        </Button>
                    </Toolbar>
                </AppBar>  
                <DialogContent>{children}</DialogContent>
            </Dialog>
        </>
    );
});

const Transition = React.forwardRef(function Transition(
    props: TransitionProps & {
        children: React.ReactElement
    },
    ref: React.Ref<unknown>,
) {
    return <Slide direction="up" ref={ref} {...props} />;
});
