import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import DialogContentText from "@mui/material/DialogContentText";
import Button  from "@mui/material/Button";
import React, { ChangeEvent, FC, memo, useCallback, useMemo, useState } from "react";

export const ShortAssociation: FC = memo(() => {

    const [input, setInput] = useState<BookInput>(() => (
        {
            name: "Learning GraphQL",
            edition: 1,
            price: 45,
            storeId: 1,
            authorIds: [1, 2]
        }
    ));

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

    const { i18n } = useDocusaurusContext();

    const isZh = useMemo(() => {
        return i18n.currentLocale == 'zh' || i18n.currentLocale == 'zh_cn' || i18n.currentLocale == 'zh_CN';
    }, [i18n.currentLocale]);

    const [dialogVisible, setDialogVisible] = useState(false);
    
    const onSubmitClick = useCallback(() => {
        setDialogVisible(true);
    }, []);
    
    const onDialogClose = useCallback(() => {
        setDialogVisible(false);
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
                <FormControl>
                    <Button onClick={onSubmitClick} variant="contained">提交</Button>
                </FormControl>
            </Stack>
            <Dialog
            open={dialogVisible}
            onClose={onDialogClose}>
                <DialogTitle>{isZh ? "信息" : "Info"}</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        {isZh ? "仅作示范，无任何效果" : "For demonstration only, without any effect"}
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onDialogClose}>关闭</Button>
                </DialogActions>
            </Dialog>
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