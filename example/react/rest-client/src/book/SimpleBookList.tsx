import { Stack } from "@mui/system";
import { DataGrid, GridActionsCellItem, GridColumns } from "@mui/x-data-grid";
import { useQuery } from "@tanstack/react-query";
import { Draft } from "immer";
import { FC, memo, useCallback, useMemo } from "react";
import { useImmer } from "use-immer";
import { api } from "../common/api";
import { RequestOf, ResponseOf } from "../__generated";
import { ElementOf } from "../__generated/ElementOf";
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';

export const SimpleBookList:FC = memo(() => {

    const getRowId = useCallback((row: Row) => row.id, []);

    const onEdit = useCallback((row: Row) => {

    }, []);

    const onDelete = useCallback((row: Row) => {

    }, []);

    const columns = useMemo<GridColumns<Row>>(() => [
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
            pageSize: 10
        };
    });

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

type Row = Draft<ElementOf<ResponseOf<typeof api.bookService.findSimpleBooks>["content"]>>;