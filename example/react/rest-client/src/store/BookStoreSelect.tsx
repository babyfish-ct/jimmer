import { Alert, CircularProgress, FormControl, InputLabel, MenuItem, Select, SelectChangeEvent } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { FC, memo, useCallback, useState } from "react";
import { api } from "../common/ApiInstance";

export const BookStoreSelect: FC<{
    readonly value?: number;
    readonly onChange: (value: number | undefined) => void
}> = memo(({value, onChange}) => {

    const [id, ] = useState(() => ++idSequnece);
    
    const { isLoading, data, error } = useQuery({
        queryKey: ["simpleBookStores"],
        queryFn: () => api.bookStoreService.findSimpleStores()
    });

    const onMuiChange = useCallback((e: SelectChangeEvent<number | undefined>) => {
        const v = e.target.value;
        if (typeof v === 'string') {
            onChange(parseInt(v));
        } else if (v === -1) {
            onChange(undefined);
        } else {
            onChange(v);
        }
    }, [onChange]);

    if (isLoading) {
        return <div><CircularProgress/>Loading items</div>
    }
    if (error) {
        return <Alert security="error">Failed to load items</Alert>
    }
    return (
        <FormControl fullWidth>
            <InputLabel id={`bookstore-select-${id}-label`}>BookStore</InputLabel>
            {
                data &&
                <Select
                labelId={`bookstore-select-${id}-label`}
                value={value}
                label="BookStore"
                onChange={onMuiChange}>
                    <MenuItem value={-1}><span style={{color: 'gray'}}>--Unspecied--</span></MenuItem>
                    {data.map(item => 
                        <MenuItem value={item.id}>{item.name}</MenuItem>
                    )}
                </Select>
            }
        </FormControl>
    );
});

let idSequnece = 0;