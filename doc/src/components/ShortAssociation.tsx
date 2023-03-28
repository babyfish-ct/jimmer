import { FormControl, InputLabel, MenuItem, Paper, Select, SelectChangeEvent, Stack, TextField } from "@mui/material";
import React, { ChangeEvent, FC, memo, useCallback, useState } from "react";

export const ShortAssociation: FC<{
    defaultValue: BookInput
}> = memo(({defaultValue}) => {

    const [input, setInput] = useState<BookInput>(defaultValue);

    const onNameChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setInput(old => ({...old, name: e.target.value}));
    }, []);

    const onEditionChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setInput(old => ({...old, edition: e.target.valueAsNumber}));
    }, []);

    const onPriceChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setInput(old => ({...old, price: e.target.valueAsNumber}));
    }, []);

    const onStoreIdChange = useCallback((e: SelectChangeEvent<number>) => {
        let storeId : number | undefined = undefined;
        const v = e.target.value;
        if (typeof v === 'string') {
            storeId = parseInt(v);
        } else {
            storeId = v;
        }
        if (storeId === -1) {
            storeId = undefined;
        }
        setInput(old => ({...old, storeId}));
    }, []);

    const onAuthorIdsChange = useCallback((e: SelectChangeEvent<number[]>) => {
        let authorIds: ReadonlyArray<number> = [];
        console.log(e.target.value);
        const v = e.target.value;
        if (typeof v === 'string') {
            authorIds = v.split(',').map(it => parseInt(it));
        } else {
            authorIds = v;
        }
        setInput(old => ({...old, authorIds}));
    }, []);

    return (
        <Paper  elevation={3} style={{padding: '1.5rem', width: 500}}>
            <Stack spacing={2}>
                    
                <h1>Book Form</h1>
                
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

                <FormControl fullWidth>
                    <InputLabel id={`store-select-label`}>Store</InputLabel>
                    <Select
                    labelId={`store-select-label`}
                    label="Authors"
                    value={input.storeId}
                    onChange={onStoreIdChange}>
                        <MenuItem value={-1}>--NONE--</MenuItem>
                        <MenuItem value={1}>O'REILLY</MenuItem>
                        <MenuItem value={2}>MANNING</MenuItem>
                    </Select>
                </FormControl>

                <FormControl fullWidth>
                    <InputLabel id={`author-multi-select-label`}>Authors</InputLabel>
                    <Select
                    multiple
                    labelId={`author-multi-select-label`}
                    label="Authors"
                    value={input.authorIds}
                    onChange={onAuthorIdsChange}>
                        <MenuItem value={1}>Eve Procello</MenuItem>
                        <MenuItem value={2}>Alex Banks</MenuItem>
                        <MenuItem value={3}>Dan Vanderkam</MenuItem>
                        <MenuItem value={4}>Boris Cherny</MenuItem>
                        <MenuItem value={5}>Samer Buna</MenuItem>
                    </Select>
                </FormControl>
                
            </Stack>
        </Paper>
    );
});

interface BookInput {
    readonly name: string;
    readonly edition: number;
    readonly price: number;
    readonly storeId?: number;
    readonly authorIds: ReadonlyArray<number>
}