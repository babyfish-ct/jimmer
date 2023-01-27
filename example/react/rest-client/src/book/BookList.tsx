import { Stack } from "@mui/system";
import { DataGrid, GridActionsCellItem, GridColumns, GridSortModel } from "@mui/x-data-grid";
import { useQuery } from "@tanstack/react-query";
import { ChangeEvent, FC, memo, MouseEvent, useCallback, useMemo, useState } from "react";
import { useImmer } from "use-immer";
import { api } from "../common/ApiInstance";
import { RequestOf } from "../__generated";
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import EditRoadIcon from '@mui/icons-material/EditRoad';
import DeleteIcon from '@mui/icons-material/Delete';
import { toSortModel, toSortCode } from "../common/SortModels";
import { Alert, Button, Chip, Drawer, Fab, TextField } from "@mui/material";
import { BookInput, BookRow, toBookInput } from "./BookTypes";
import { BookForm } from "./BookForm";
import { BookDeleter } from "./BookDeleter";
import { useTenant } from "../dashboard/TenantContext";
import { CompositeBookForm } from "./CompositeBookForm";

export const BookList:FC = memo(() => {

    const getRowId = useCallback((row: BookRow) => row.id, []);

    const [selectedRow, setSelectedRow] = useState<BookRow>();

    const [saveMode, setSaveMode] = useState<'NONE' | 'GENERIC' | 'COMPOSITE'>('NONE');

    const [deletedRow, setDeletedRow] = useState<BookRow>();

    const [popAnchorEl, setPopAnchorEl] = useState<HTMLButtonElement>();

    const tenant = useTenant();

    const onAdd = useCallback(() => {
        setSelectedRow(undefined);
        setSaveMode('GENERIC');
    }, []);

    const onEdit = useCallback((row: BookRow) => {
        setSelectedRow(row);
        setSaveMode('GENERIC');
    }, []);

    const onEditComposite = useCallback((row: BookRow) => {
        setSelectedRow(row);
        setSaveMode('COMPOSITE');
    }, []);

    const onDelete = useCallback((row: BookRow, e: MouseEvent<HTMLButtonElement>) => {
        setDeletedRow(row);
        setPopAnchorEl(e.currentTarget);
    }, []);

    const columns = useMemo<GridColumns<BookRow>>(() => [
        {
            field: "id",
            headerName: "ID"
        },
        {
            field: "name",
            headerName: "Name",
            width: 200
        },
        {
            field: "edition",
            headerName: "Edition"
        },
        {
            field: "price",
            headerName: "Price"
        },
        {
            field: "store.name",
            headerName: "Store",
            width: 150,
            renderCell: params => (
                <Chip label={params.row.store?.name}/>
            )
        },
        {
            field: "authors.forEach.fullName",
            headerName: "Authors",
            flex: 1,
            sortable: false,
            renderCell: params => (
                <>
                    {params.row.authors.map(author => (
                        <Chip label={`${author.firstName} ${author.lastName}`}/>
                    ))}
                </>
            )
        },
        { 
            field: 'actions', 
            headerName: 'Actions', 
            type: 'actions',
            getActions: params => [
                <GridActionsCellItem icon={<EditIcon/>} label="Edit" onClick={() => onEdit(params.row)} disabled={tenant === undefined}/>,
                <GridActionsCellItem icon={<EditRoadIcon/>} label="EditComposite" onClick={() => onEditComposite(params.row)} disabled={tenant === undefined}/>,
                <GridActionsCellItem icon={<DeleteIcon/>} label="Delete" onClick={e => onDelete(params.row, e)}/>
            ]
        }
    ], [onEdit, onDelete, tenant]);

    const [options, setOptions] = useImmer<RequestOf<typeof api.bookService.findBooks>>(() => {
        return {
            pageIndex: 0,
            pageSize: 10,
            sortCode: "name asc"
        };
    });

    const onNameChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setOptions(draft => {
            draft.name = e.target.value;
        })
    }, [setOptions]);

    const onStoreNameChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setOptions(draft => {
            draft.storeName = e.target.value;
        })
    }, [setOptions]);

    const onAuhtorChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setOptions(draft => {
            draft.authorName = e.target.value;
        })
    }, [setOptions]);

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

    const { isLoading, data, error, refetch } = useQuery({
       queryKey: ["Books", options],
       queryFn: () => api.bookService.findBooks(options) 
    });

    const onRefreshClick = useCallback(() => {
        refetch();
    }, [refetch]);

    const onFormClose = useCallback(() => {
        setSaveMode('NONE');
    }, []);

    const onPopClose = useCallback((row: BookRow | undefined) => {
        setDeletedRow(undefined);
        setPopAnchorEl(undefined);
    }, []);

    return (
        <Stack spacing={2}>
            {tenant === undefined && <Alert severity="warning">Add/Edit can only be enabled when global tenant is set, please enter it(eg: a)</Alert>}
            <Stack direction="row" spacing={2}>
                <TextField label="Search by book name" value={options.name} onChange={onNameChange}/>
                <TextField label="Search by store name" value={options.storeName} onChange={onStoreNameChange}/>
                <TextField label="Search by author name" value={options.authorName} onChange={onAuhtorChange}/>
                <Button variant="outlined" onClick={onRefreshClick}>Refresh</Button>
            </Stack>
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
            disableSelectionOnClick
            disableColumnFilter={true}/>
            <Fab color="primary" onClick={onAdd} disabled={tenant === undefined}>
                <AddIcon/>
            </Fab>
            <Drawer open={saveMode === 'GENERIC'} anchor="right">
                <BookForm value={selectedRow !== undefined ? toBookInput(selectedRow) : undefined} onClose={onFormClose}/>
            </Drawer>
            <Drawer open={saveMode === 'COMPOSITE'} anchor="right">
                <CompositeBookForm id={selectedRow?.id} onClose={onFormClose}/>
            </Drawer>
            <BookDeleter row={deletedRow} anchorEl={popAnchorEl} onClose={onPopClose}/>
        </Stack>
    );
});
