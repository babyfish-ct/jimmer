import { Stack } from "@mui/system";
import { DataGrid, GridActionsCellItem, GridColumns, GridSortModel } from "@mui/x-data-grid";
import { useQuery } from "@tanstack/react-query";
import { FC, memo, useCallback, useMemo } from "react";
import { useImmer } from "use-immer";
import { api } from "../common/ApiInstance";
import { RequestOf } from "../__generated";
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { toSortModel, toSortCode } from "../common/SortModels";
import { SimpleBook } from "./BookTypes";

export const SimpleBookList:FC = memo(() => {

    const getRowId = useCallback((row: SimpleBook) => row.id, []);

    const onEdit = useCallback((row: SimpleBook) => {

    }, []);

    const onDelete = useCallback((row: SimpleBook) => {

    }, []);

    const columns = useMemo<GridColumns<SimpleBook>>(() => [
        {
            field: "id",
            headerName: "ID",
        },
        {
            field: "name",
            headerName: "Name",
            width: 200
        },
        { 
            field: 'actions', 
            headerName: 'Actions', 
            type: 'actions',
            getActions: params => [
                <GridActionsCellItem icon={<EditIcon/>} label="Edit" onClick={() => onEdit(params.row)}/>,
                <GridActionsCellItem icon={<DeleteIcon/>} label="Delete" onClick={() => onDelete(params.row)}/>
            ]
        }
    ], [onEdit, onDelete]);

    const [options, setOptions] = useImmer<RequestOf<typeof api.bookService.findSimpleBooks>>(() => {
        return {
            pageIndex: 0,
            pageSize: 10,
            sortCode: "name asc"
        };
    });

    const onSortModelChange = useCallback((model: GridSortModel) => {
        setOptions(draft => {
            draft.sortCode = toSortCode(model);
        });
    }, [setOptions]);

    const onPageChange = useCallback((page: number) => {
        setOptions(draft => {
            draft.pageIndex = page;
        });
    }, [setOptions]);

    const { isLoading, data, error } = useQuery({
       queryKey: ["simpleBooks", options],
       queryFn: () => api.bookService.findSimpleBooks(options) 
    });

    return (
        <Stack spacing={2}>
            <DataGrid
            getRowId={getRowId}
            columns={columns}
            loading={isLoading}
            rows={data?.content ?? []}
            error={error}
            sortingMode="server"
            sortModel={toSortModel(options.sortCode)}
            onSortModelChange={onSortModelChange}
            pagination
            paginationMode="server"
            rowCount={data?.totalElements ?? 0}
            pageSize={options.pageSize}
            onPageChange={onPageChange}
            autoHeight
            disableSelectionOnClick/>
        </Stack>
    );
});
