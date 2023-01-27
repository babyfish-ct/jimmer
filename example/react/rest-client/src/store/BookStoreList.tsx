import { DataGrid, GridColumns } from "@mui/x-data-grid";
import { useQuery } from "@tanstack/react-query";
import { FC, memo, useCallback, useMemo } from "react";
import { api } from "../common/ApiInstance";
import { Stack } from "@mui/system";
import { Fab } from "@mui/material";
import AddIcon from '@mui/icons-material/Add';
import { BookStoreRow } from "./BookStoreTypes";

export const BookStoreList: FC = memo(() => {

    const { isLoading, data, error } = useQuery({
        queryKey: ["simpleBookStores"],
        queryFn: () => api.bookStoreService.findStores()
    });

    const getRowId = useCallback((row: BookStoreRow) => row.id, []);

    const columns = useMemo<GridColumns>(() => [
        { field: 'id', headerName: 'Id', type: 'number' },
        { field: 'name', headerName: 'Name', type: 'string' },
    ], []);

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
            <Fab color="primary">
                <AddIcon/>
            </Fab>
        </Stack>
    );
});