import { AppBar, Button, Dialog, DialogContent, Toolbar, Typography } from "@mui/material";
import React, { FC, memo, PropsWithChildren, useCallback, useState } from "react";
import Slide from '@mui/material/Slide';
import { TransitionProps } from '@mui/material/transitions';
import IconButton from '@mui/material/IconButton';
import OpenInFullIcon from '@mui/icons-material/OpenInFull';
import CloseFullscreenIcon from '@mui/icons-material/CloseFullscreen';
import CloseIcon from '@mui/icons-material/Close';

export const ViewMore: FC<
    PropsWithChildren<{
        readonly buttonText: string,
        readonly fullScreen?: boolean,
        readonly title?: string
    }>
> = memo(({buttonText, fullScreen = false, title = buttonText, children}) => {
    
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
            <Dialog 
            open={open} 
            onClose={onClose} 
            fullScreen={maximize} 
            TransitionComponent={Transition}
            maxWidth="md">
                <AppBar sx={{ position: 'relative' }}>
                    <Toolbar>
                        <Typography sx={{ ml: 2, flex: 1 }} variant="h6" component="div">
                            {title}
                        </Typography>
                        <IconButton onClick={onResize} style={{color:'white'}}>
                            {maximize ? <CloseFullscreenIcon/> : <OpenInFullIcon/> }
                        </IconButton>
                        <IconButton aria-label="close" onClick={onClose} style={{color:'white'}}>
                            <CloseIcon/>
                        </IconButton>
                    </Toolbar>
                </AppBar>
                <DialogContent>
                    {children}
                </DialogContent>
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
