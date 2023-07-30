import React, { FC, PropsWithChildren, memo, useCallback, useState } from "react";
import AppBar from "@mui/material/AppBar";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import Slide from "@mui/material/Slide";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { TransitionProps } from "@mui/material/transitions";
import IconButton from '@mui/material/IconButton';
import OpenInFullIcon from '@mui/icons-material/OpenInFull';
import CloseFullscreenIcon from '@mui/icons-material/CloseFullscreen';
import CloseIcon from '@mui/icons-material/Close';

export const ViewDialog: FC<
    PropsWithChildren<{
        readonly open: boolean,
        readonly fullScreen?: boolean,
        readonly title: string,
        readonly onClose: () => void
    }>
> = memo(({open, fullScreen = false, title, onClose, children}) => {

    const [maximize, setMaximize] = useState(fullScreen);
    const onResize = useCallback(() => {
        setMaximize(old => !old);
    }, []);

    return ( 
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