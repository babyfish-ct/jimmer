import { DataGrid, GridActionsCellItem, GridColDef, GridColumns } from "@mui/x-data-grid";
import { useQuery } from "@tanstack/react-query";
import { Draft } from "immer";
import { FC, memo, useCallback, useMemo } from "react";
import { api } from "../common/api";
import { ResponseOf } from "../__generated";
import { ElementOf } from "../__generated/ElementOf";
import { Stack } from "@mui/system";
import { Fab } from "@mui/material";
import AddIcon from '@mui/icons-material/Add';

export const SimpleBookStoreList: FC = memo(() => {

    const { isLoading, data, error } = useQuery({
        queryKey: ["simpleBookStores"],
        queryFn: () => api.bookStoreService.findSimpleStores()
    });

    const getRowId = useCallback((row: Row) => row.id, []);

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

type Row = Draft<ElementOf<ResponseOf<typeof api.bookStoreService.findSimpleStores>>>;