import React, { ChangeEvent, FC, memo, useCallback, useMemo, useState } from "react";
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Grid from '@mui/material/Grid';
import Divider from '@mui/material/Divider';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TableFooter from '@mui/material/TableFooter';
import AddIcon from '@mui/icons-material/Add';
import IconButton from '@mui/material/IconButton';
import DeleteIcon from '@mui/icons-material/Delete';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import { useImmer } from "use-immer";
import { Draft } from "immer";
import { useZh } from "../util/use-zh";

export const LongAssociation: FC = memo(() => {
    
    const { i18n } = useDocusaurusContext();

    const isZh = useZh();

    const users = useMemo(() => {
        return isZh ? ZH_USERS : EN_USERS
    }, [isZh]);
    
    const userMap = useMemo<Map<number, User>>(() => {
        const map = new Map<number, User>();
        for (const user of users) {
            map.set(user.id, user);
        }
        return map;
    }, [users]);
    
    
    const products = useMemo(() => {
        return isZh ? ZH_PRODUCTS : EN_PRODUCTS
    }, [isZh]);

    const productMap = useMemo<Map<number, Product>>(() => {
        const map = new Map<number, Product>();
        for (const product of products) {
            map.set(product.id, product);
        }
        return map;
    }, [products]);

    const [order, setOrder] = useImmer<Order>(() => {
        return ({
            userId: 1,
            province: userMap.get(1).province,
            city: userMap.get(1).city,
            address: userMap.get(1).address,
            items: [
                { __rowId: 1, productId: 1, name: productMap.get(1).name, quantity: 2 },
                { __rowId: 2, productId: 10, name: productMap.get(10).name, quantity: 1 }
            ]
        });
    });

    const nextRowId = useMemo<number>(() => {
        return Math.max(...order.items.map(item => item.__rowId)) + 1
    }, [order]);

    const onUserChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        const user = userMap.get(parseInt(e.target.value));
        if (user !== undefined) {
            setOrder(order => {
                order.userId = user.id;
                order.province = user.province;
                order.city = user.city;
                order.address = user.address;
            })
        }
    }, [setOrder, userMap]);

    const onPrivinceChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setOrder(order => {
            order.province = e.target.value;
        });
    }, [setOrder]);

    const onCityChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setOrder(order => {
            order.city = e.target.value;
        });
    }, [setOrder]);

    const onAddressChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
        setOrder(order => {
            order.address = e.target.value;
        });
    }, [setOrder]);

    const onProductChange = useCallback((e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>, rowId: number) => {
        setOrder(order => {
            for (const item of order.items) {
                if (item.__rowId === rowId) {
                    item.productId = parseInt(e.target.value);
                    normalizeOrder(order);
                    break;
                }
            }
        });
    }, [setOrder]);

    const onQuantityChange = useCallback((e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>, rowId: number) => {
        setOrder(order => {
            for (const item of order.items) {
                if (item.__rowId === rowId) {
                    item.quantity = parseInt(e.target.value);
                    normalizeOrder(order);
                    break;
                }
            }
        });
    }, [setOrder]);

    const onDeleteClick = useCallback((rowId: number) => {
        setOrder(order => {
            for (let i = order.items.length - 1; i >= 0; --i) {
                if (order.items[i].__rowId === rowId) {
                    order.items.splice(i, 1);
                    break;
                }
            }
        });
    }, [setOrder]);

    const onAddClick = useCallback(() => {
        setOrder(order => {
            order.items.push({__rowId: nextRowId, quantity: 1});
        })
    }, [setOrder, nextRowId]);

    const [dialogVisible, setDialogVisible] = useState(false);
    
    const onSubmitClick = useCallback(() => {
        setDialogVisible(true);
    }, []);
    
    const onDialogClose = useCallback(() => {
        setDialogVisible(false);
    }, []);

    return (
        <Paper  elevation={3} style={{padding: '1.5rem'}}>
            <Box
            component="form"
            noValidate
            autoComplete="off">
                <Grid container spacing={2}>
                    <Grid item xs={4}>
                        <TextField
                        fullWidth
                        select
                        label={isZh ? "购买人" : "Purchaser"}
                        value={order.userId}
                        onChange={onUserChange}>
                        {
                            users.map(
                                user => (
                                    <MenuItem key={user.id} value={user.id}>
                                        {user.nickName}
                                    </MenuItem>
                                )
                            )
                        }
                        </TextField>
                    </Grid>
                    <Grid item xs={4}>
                        <TextField
                        fullWidth
                        label={isZh ? "省份" : "Province"}
                        value={order.province}
                        onChange={onPrivinceChange}/>
                    </Grid>
                    <Grid item xs={4}>
                        <TextField
                        fullWidth
                        label={isZh ? "城市" : "City"}
                        value={order.city}
                        onChange={onCityChange}/> 
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                        fullWidth
                        label={isZh ? "地址" : "Address"}
                        value={order.address}
                        onChange={onAddressChange}/>
                    </Grid>
                    <Grid item xs={12}>
                        <Divider textAlign="left">订单明细</Divider>
                        <TableContainer component={Paper}>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>{isZh ? "商品" : "Commodity"}</TableCell>
                                        <TableCell>{isZh ? "数量" : "Quantity"}</TableCell>
                                        <TableCell>{isZh ? "单价" : "Unit price"}</TableCell>
                                        <TableCell>{isZh ? "明细价" : "Item price"}</TableCell>
                                        <TableCell>{isZh ? "删除" : "Delete"}</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {
                                        order.items.map(item => {
                                            const product = item.productId !== undefined ? productMap.get(item.productId) : undefined;
                                            return (
                                                <TableRow key={item.__rowId}>
                                                    <TableCell>
                                                        <TextField
                                                        select
                                                        value={product?.id}
                                                        variant="standard"
                                                        size="small"
                                                        onChange={e => {onProductChange(e, item.__rowId)}}>
                                                            {
                                                                products.map(p => (
                                                                    <MenuItem key={p.id} value={p.id}>
                                                                        {p.name}
                                                                    </MenuItem>
                                                                ))
                                                            }
                                                        </TextField>
                                                    </TableCell>
                                                    <TableCell>
                                                    <TextField
                                                        type="number"
                                                        value={item.quantity}
                                                        variant="standard"
                                                        size="small"
                                                        onChange={e => {onQuantityChange(e, item.__rowId)}}/>
                                                    </TableCell>
                                                    <TableCell>
                                                        {product?.price}
                                                    </TableCell>
                                                    <TableCell>
                                                        {product !== undefined && product.price * item.quantity }
                                                    </TableCell>
                                                    <TableCell>
                                                    <IconButton onClick={() => onDeleteClick(item.__rowId)}>
                                                        <DeleteIcon />
                                                    </IconButton>
                                                    </TableCell>
                                                </TableRow>
                                            );
                                        })
                                    }
                                </TableBody>
                                <TableFooter>
                                    <TableRow>
                                        <TableCell colSpan={5}>
                                            <Button onClick={onAddClick}>
                                                <AddIcon/>{isZh ? "添加" : "Add"}
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                </TableFooter>
                            </Table>
                        </TableContainer>
                    </Grid>
                    <Grid item xs={12}>
                        { isZh ? "总额" : "Total price" } 
                        : 
                        { order
                            .items
                            .filter(item => item.productId !== undefined && typeof item.quantity === "number") 
                            .map(item => productMap.get(item.productId!!)!!.price * item.quantity)
                            .reduce((prev, cur) => prev + cur, 0)
                        }
                    </Grid>
                    <Grid item xs={12}>
                        <Button variant="contained" onClick={onSubmitClick}>{isZh ? "提交" : "Submit"}</Button>
                    </Grid>
                </Grid>
            </Box>
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
                    <Button onClick={onDialogClose}>{isZh ? "关闭" : "Close"}</Button>
                </DialogActions>
            </Dialog>
        </Paper>
    );
});

interface Order {
    readonly userId: number;
    readonly province: string;
    readonly city: string;
    readonly address: string;
    readonly items: ReadonlyArray<OrderItem>;
}

interface OrderItem {
    readonly __rowId: number;
    readonly productId?: number;
    readonly quantity: number;
}

interface User {
    readonly id: number;
    readonly nickName: string;
    readonly province: string;
    readonly city: string;
    readonly address: string;
}

interface Product {
    readonly id: number;
    readonly name: string;
    readonly price: number;
}

const ZH_USERS: ReadonlyArray<User> = [
    {id: 1, nickName: "皮皮鲁", province: "四川", city: "成都", address: "龙泉驿区洪玉路与十洪路交叉口"},
    {id: 2, nickName: "鲁西西", province: "广东", city: "广州", address: "白云区石沙路300号"},
    {id: 3, nickName: "舒克", province: "西藏", city: "拉萨", address: "堆龙德庆区"},
    {id: 4, nickName: "贝塔", province: "上海", city: "上海", address: "浦东新区秀沿西路218弄"},
];

const EN_USERS: ReadonlyArray<User> = [
    {id: 1, nickName: "Schneewittchen", province: "Berlin", city: "Prenzlauer Berg", address: "Brandenburgische Straße 9, Prenzlauer Berg, Berlin, Germany"},
    {id: 2, nickName: "Cinderella", province: "Basse-Normandie", city: "Lisieux", address: "20 rue Léon Dierx, Lisieux, Basse-Normandie, France"},
    {id: 3, nickName: "Nuwa", province: "ShanXi", city: "Taiyuan", address: "Qian Feng Nan Lu 99hao, Taiyuan, ShanXi, China"},
    {id: 4, nickName: "Pinocchio", province: "Palazzo Pignano", city: "Cremona", address: "Via Giovanni Amendola 134, Palazzo Pignano, Cremona, Italy"},
];

const ZH_PRODUCTS: ReadonlyArray<Product> = [
    {id: 1, name: "zippo夜光流沙打火机", price: 268},
    {id: 2, name: "杰登(Jayden)印度塔布拉鼓", price: 9238},
    {id: 3, name: "浪琴(Longines)机械手表", price: 13900},
    {id: 4, name: "viney男士皮带", price: 139},
    {id: 5, name: "雅诗兰黛绒雾哑光唇膏", price: 310},
    {id: 6, name: "CIRCUIT男子滑雪单板", price: 2044},
    {id: 7, name: "特仑苏脱脂牛奶", price: 47},
    {id: 8, name: "乐高积木布加迪", price: 374},
    {id: 9, name: "双喜燃气电磁通用压力锅", price: 137},
    {id: 10, name: "憨憨宠猫爬架", price: 238},
];

const EN_PRODUCTS: ReadonlyArray<Product> = [
    {id: 1, name: "Timeless Vitamin C Plus E 10 Percent Ferulic Acid Serum Serum Unisex 1 oz", price: 14.69},
    {id: 2, name: "Dockers Men's Tiller Boat Shoe", price: 52.41},
    {id: 3, name: "Paxcoo 124 Skeins Embroidery Floss Cross Stitch Thread with Needles", price: 29.99},
    {id: 4, name: "Michael Kors Crossbody", price: 70.6},
    {id: 5, name: "Crocs unisex-child Classic Graphic Clog", price: 34.99},
    {id: 6, name: "Timex Time Machines Digital 35mm Watch", price: 28.95},
    {id: 7, name: "Scalex Mite & Lice Spray for Birds, 8 oz.", price: 14.75},
    {id: 8, name: "Under Armour Men's Storm Liner", price: 13.03},
    {id: 9, name: "Nike Women's Pro 3\" Training Shorts", price: 35.86},
    {id: 10, name: "Olaplex No. 4 Bond Maintenance Shampoo", price: 30},
];

function normalizeOrder(order: Draft<Order>) {
    const items = order.items;
    const existingItemMap = new Map<number, Draft<OrderItem>>();
    for (let i = 0; i < items.length; i++) {
        const item = items[i];
        if (typeof item.quantity !== "number") {
            item.quantity = 1;
        } else if (item.quantity < 1) {
            items.splice(i--, 1);
            continue;
        }
        if (item.productId === undefined) {
            continue;
        }
        const existingItem = existingItemMap.get(item.productId);
        if (existingItem === undefined) {
            existingItemMap.set(item.productId, item);
        } else {
            existingItem.quantity += item.quantity;
            items.splice(i--, 1);
        }
    }
}