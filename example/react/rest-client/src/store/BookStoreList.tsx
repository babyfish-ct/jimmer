import { DataGrid, GridActionsCellItem, GridColumns } from "@mui/x-data-grid";
import { useQuery } from "@tanstack/react-query";
import { FC, memo, MouseEvent, useCallback, useMemo, useState } from "react";
import { api } from "../common/ApiInstance";
import { Stack } from "@mui/system";
import { Drawer, Fab } from "@mui/material";
import AddIcon from '@mui/icons-material/Add';
import { BookStoreRow } from "./BookStoreTypes";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import { BookStoreForm } from "./BookStoreForm";
import { BookStoreDeleter } from "./BookStoreDeleter";

export const BookStoreList: FC = memo(() => {

    const { isLoading, data, error } = useQuery({
        queryKey: ["simpleBookStores"],
        queryFn: () => api.bookStoreService.findStores()
    });

    const getRowId = useCallback((row: BookStoreRow) => row.id, []);

    const [editingRow, setEditingRow] = useState<BookStoreRow>();

    const [save, setSave] = useState(false);

    const [popAnchorEl, setPopAnchorEl] = useState<HTMLButtonElement>();

    const [deletingRow, setDeletingRow] = useState<BookStoreRow>();

    const onAdd = useCallback(() => {
        setEditingRow(undefined);
        setSave(true);
    }, []);

    const onEdit = useCallback((row: BookStoreRow) => {
        setEditingRow(row);
        setSave(true);
    }, []);

    const onDelete = useCallback((row: BookStoreRow, e: MouseEvent<HTMLButtonElement>) => {
        setDeletingRow(row);
        setPopAnchorEl(e.currentTarget);
    }, []);

    const onFormClose = useCallback(() => {
        setSave(false);
    }, []);

    const onPopClose = useCallback(() => {
        setDeletingRow(undefined);
    }, []);

    const columns = useMemo<GridColumns>(() => [
        { field: 'id', headerName: 'Id', type: 'number' },
        { field: 'name', headerName: 'Name', type: 'string', flex: 1 },
        { field: 'website', headerName: 'Website', type: 'string', flex: 2 },
        { 
            field: 'actions', 
            headerName: 'Actions',
            type: 'actions',
            getActions: params => [
                <GridActionsCellItem icon={<EditIcon/>} label="Edit" onClick={() => onEdit(params.row)}/>,
                <GridActionsCellItem icon={<DeleteIcon/>} label="Delete" onClick={e => onDelete(params.row, e)}/>
            ]
        }
    ], [onEdit, onDelete]);

    return (
        <Stack spacing={2}>
            
            <DataGrid 
            getRowId={getRowId}
            loading={isLoading}
            error={error} 
            columns={columns} 
            rows={data ?? []}
            autoHeight
            hideFooter
            disableSelectionOnClick/>
            
            <Fab color="primary" onClick={onAdd}>
                <AddIcon/>
            </Fab>

            <Drawer open={save} anchor="right">
                <div style={{padding: '1rem', width: 400}}>
                    <BookStoreForm value={editingRow} onClose={onFormClose}/>
                </div>
            </Drawer>

            <BookStoreDeleter row={deletingRow} anchorEl={popAnchorEl} onClose={onPopClose}/>
        </Stack>
    );
});