import { Button, CircularProgress, Stack, TextField } from "@mui/material";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ChangeEvent, FC, memo, useCallback, useMemo, useState } from "react";
import { useImmer } from "use-immer";
import { api } from "../common/ApiInstance";
import { MessageDialog, MessageInfo } from "../common/MessageDialog";
import { BookStoreInput } from "../__generated/model/static";

export const BookStoreForm: FC<{
    value?: BookStoreInput,
    onClose: (value: BookStoreInput | undefined) => void
}> = memo(({value, onClose}) => {

    const [input, setInput] = useImmer<BookStoreInput>(value ?? {
        name: ""
    });

    const onNameChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setInput(draft => {
            draft.name = e.target.value;
        });
    }, [setInput]);

    const onWebsiteChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        const v = e.target.value;
        setInput(draft => {
            draft.website = v !== "" ? v : undefined;
        });
    }, [setInput]);

    const isInvalid = useMemo(() => {
        return input.name === ""; 
    }, [input]);

    const queryClient = useQueryClient();

    const { isLoading, mutateAsync } = useMutation({
        mutationFn: () => api.bookStoreService.saveBookStore({body: input}),
        onSettled: () => queryClient.invalidateQueries(),
    });

    const [messageInfo, setMessageInfo] = useState<MessageInfo>();

    const onSaveClick = useCallback(async () => {
        try {
            await mutateAsync();
        } catch (ex) {
            setMessageInfo({
                severity: "error", 
                title: "Error", 
                content: "Failed to save BookStore"
            });
            return;
        }
        setMessageInfo({ 
            title: "Success", 
            content: "BookStore has been saved"
        });
    }, [mutateAsync]);

    const onCancelClick = useCallback(() => {
        onClose(undefined);
    }, [onClose]);

    const onMessageClose = useCallback(() => {
        setMessageInfo(info => {
            if (info?.severity !== "error") {
                onClose(input);
            }
            return undefined;
        });
    }, [input, onClose]);

    return (
        <Stack spacing={2}>

            <h1>{value !== undefined ? "Edit" : "Create"} book store</h1>
            
            <TextField 
            value={input.name} 
            onChange={onNameChange}
            error={input.name === ""}
            helperText={input.name === "" ? "Name is required" : ""}/>
            
            <TextField 
            type="url"
            value={input.website} 
            onChange={onWebsiteChange}/>

            <Stack direction="row" spacing={1}>
                <Button variant="contained" onClick={onSaveClick} disabled={isInvalid}>
                    {isLoading ? <CircularProgress size="small"/> : "Save"}
                </Button>
                <Button variant="outlined" onClick={onCancelClick}>Cancel</Button>
            </Stack>

            <MessageDialog info={messageInfo} onClose={onMessageClose}/>

        </Stack>
    );
});