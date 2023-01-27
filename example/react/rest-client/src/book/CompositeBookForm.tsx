import { Button, CircularProgress, Divider, InputLabel, Stack, TextField } from "@mui/material";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ChangeEvent, FC, memo, useCallback, useMemo, useState } from "react";
import { useImmer } from "use-immer";
import { AuthorMultiSelect } from "../author/AuthorMultiSelect";
import { MessageInfo, MessageDialog } from "../common/MessageDialog";
import { api } from "../common/ApiInstance";
import { BookStoreSelect } from "../store/BookStoreSelect";
import { CompositeBookInput } from "../__generated/model/static";
import { DataGrid, GridColumns } from "@mui/x-data-grid";

export const CompositeBookForm: FC<{
    id?: number,
    onClose: (value: CompositeBookInput | undefined) => void
}> = memo(({id, onClose}) => {

    const [input, setInput] = useImmer<CompositeBookInput>({
        name: "",
        edition: 1,
        price: 50,
        authorIds: [],
        chapters: []
    });

    const queryClient = useQueryClient();

    const { isLoading, mutateAsync } = useMutation({
        mutationFn: () => api.bookService.saveBook({body: input}),
        onSuccess: () => queryClient.invalidateQueries()
    });

    const onNameChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setInput(draft => {
            draft.name = e.target.value;
        });
    }, [setInput]);

    const onEditionChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setInput(draft => {
            draft.edition = e.target.valueAsNumber;
        });
    }, [setInput]);

    const onPriceChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setInput(draft => {
            draft.price = e.target.valueAsNumber;
        });
    }, [setInput]);

    const onStoreIdChange = useCallback((value: number | undefined) => {
        setInput(draft => {
            draft.storeId = value;
        });
    }, [setInput]);

    const onAuthorIdsChange = useCallback((value: Array<number>) => {
        setInput(draft => {
            draft.authorIds = value;
        });
    }, [setInput]);

    const [alertInfo, setAlertInfo] = useState<MessageInfo>();

    const onSaveClick = useCallback(async () => {
        try {
            await mutateAsync();
        } catch (ex) {
            setAlertInfo({
                severity: "error", 
                title: "Error", 
                content: "Failed to save book"
            });
            return;
        }
        setAlertInfo({ 
            title: "Success", 
            content: "Book has been saved"
        });
    }, [mutateAsync]);

    const onCancelClick = useCallback(() => {
        onClose(undefined);
    }, [onClose]);

    const onAlertClose = useCallback(() => {
        setAlertInfo(info => {
            if (info?.severity !== "error") {
                onClose(input);
            }
            return undefined;
        });
    }, [input, onClose]);

    const isInvalid = useMemo<boolean>(() => {
        return input.name === "";
    }, [input]);

    const columns = useMemo<GridColumns>(() => [
        { field: 'title', headerName: 'Title', type: 'string' },
    ], []);

    return (
        <div style={{padding: '1rem', width: 400}}>
            <Stack spacing={2}>
                
                <h1>{id !== undefined ? "Edit" : "Create"} book and chapters</h1>
                
                <TextField 
                label="Name" 
                value={input.name} 
                onChange={onNameChange}
                error={input.name === ""}
                helperText={input.name === "" ? "Name is required" : ""}/>
                
                <TextField 
                label="Edition" 
                type="number"
                value={input.edition} 
                onChange={onEditionChange}/>
                
                <TextField 
                label="Price" 
                type="number"
                value={input.price} 
                onChange={onPriceChange}/>

                <BookStoreSelect value={input.storeId} onChange={onStoreIdChange}/>

                <AuthorMultiSelect value={input.authorIds} onChange={onAuthorIdsChange}/>

                <Divider/>

                <DataGrid 
                columns={columns}
                rows={input.chapters} 
                pagination={undefined} 
                hideFooter={true} 
                disableColumnFilter={true} 
                disableSelectionOnClick={true}/>

                <Stack direction="row" spacing={1} alignContent="center" style={{width: '100%'}}>
                    <Button variant="contained" disabled={isInvalid || isLoading} onClick={onSaveClick}>
                        {isLoading ? <CircularProgress size="small"/> : "Save"}
                    </Button>
                    <Button variant="outlined" onClick={onCancelClick}>Cancel</Button>
                </Stack>

                <MessageDialog info={alertInfo} onClose={onAlertClose}/>
            </Stack>
        </div>
    );
});

