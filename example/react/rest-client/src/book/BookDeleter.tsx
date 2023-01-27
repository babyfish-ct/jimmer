import { Button, Popover, Stack } from "@mui/material";
import { FC, memo, useCallback } from "react";
import { BookRow } from "./BookTypes";

export const BookDeleter: FC<{
    readonly row?: BookRow,
    readonly anchorEl?: HTMLElement,
    readonly onClose: (row: BookRow | undefined) => void
}> = memo(({row, anchorEl, onClose}) => {
    
    const onNoClick = useCallback(() => {
        onClose(row);
    }, [onClose, row]);

    return (
        <Popover 
        open={row !== undefined && anchorEl !== undefined}
        anchorOrigin={{
            horizontal: 'right',
            vertical: 'bottom'
        }}
        onClose={onNoClick}
        anchorEl={anchorEl}>
            <div style={{padding: '1rem', width: 300}}>
                <Stack spacing={1}>
                    <div>Are you sure to delete book "{row?.name}"(edition {row?.edition})?</div>
                    <Stack direction="row" spacing={1} alignContent="center" style={{width: '100%'}}>
                        <Button variant="outlined" onClick={onNoClick}>Yes</Button>
                        <Button variant="outlined" onClick={onNoClick}>No</Button>
                    </Stack>
                </Stack>
            </div>
        </Popover>
    );
});