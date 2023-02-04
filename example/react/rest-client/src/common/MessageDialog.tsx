import { Alert, Button, Dialog, DialogActions, DialogContentText, DialogTitle, AlertColor, Slide } from "@mui/material";
import { TransitionProps } from "@mui/material/transitions";
import { FC, forwardRef, memo, ReactElement, ReactNode } from "react";

export interface MessageInfo {
    readonly severity?: AlertColor;
    readonly title: string;
    readonly content: ReactNode;
}

export const MessageDialog: FC<{
    readonly info?: MessageInfo,
    readonly onClose: () => void
}> = memo(({info, onClose}) => {

    return (
        <Dialog open={info !== undefined} TransitionComponent={Transition}>
            <DialogTitle>
                {info?.title}
            </DialogTitle>
            <DialogContentText>
                <div style={{padding: '1rem', width: 400}}>
                    <Alert severity={info?.severity ?? "info"}>{info?.content}</Alert>
                </div>
            </DialogContentText>
            <DialogActions>
                <Button onClick={onClose}>Close</Button>
            </DialogActions>
        </Dialog>
    );
});

const Transition = forwardRef(function(
    props: TransitionProps & {
      children: ReactElement<any, any>;
    },
    ref: React.Ref<unknown>,
) {
    return <Slide direction="up" ref={ref} {...props} />;
});