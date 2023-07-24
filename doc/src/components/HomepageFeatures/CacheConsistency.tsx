import React, { FC, ReactNode, memo } from "react";
import { ViewMore } from "../ViewMore";
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import { useZh } from "@site/src/util/use-zh";
import { ViewDialog } from "../ViewDialog";

export const CacheConsistency: FC = memo(() => {
    const zh = useZh();
    return zh ? 
        <ViewMore buttonText='简要了解' title='缓存一致性' variant='outlined'>
            {ZH}
        </ViewMore> : 
        <ViewMore buttonText='A Brief Introduction' title='Cache Consistency' variant='outlined'>
            {EN}
        </ViewMore>;
});

export const CacheConsistencyDialog: FC<{
    readonly open: boolean,
    readonly onClose: () => void
}> = memo(props => {
    const zh = useZh();
    return zh ? 
        <ViewDialog title='缓存一致性' {...props}>
            {ZH}
        </ViewDialog> : 
        <ViewDialog title='Cache Consistency' {...props}>
            {EN}
        </ViewDialog>;
});

export const CacheConsistencyPanel: FC = memo(() => {
    const zh = useZh();
    return zh ? ZH : EN;
});

const Consistency = require("@site/static/img/consistency.jpg").default;

const ZH: ReactNode = 
    <>
        <p><img src={Consistency}/></p>
        <p>
            <b>左侧</b>：修改数据库中的数据，将<code>Book-10</code>的外建字段<code>STORE_ID</code>从<code>2</code>修改为<code>1</code>
        </p>
        <p>
            <b>右侧</b>：此举会导致连锁反应，Jimmer会自动清理所有受影响的缓存，清理缓存的动作会在IDE中留下日志记录
            <div style={{padding: '1rem', marginLeft: '2rem'}}>
                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>
                                    被清理的缓存名
                                </TableCell>
                                <TableCell>
                                    <span style={{whiteSpace: "nowrap"}}>缓存类型</span>
                                </TableCell>
                                <TableCell>
                                    描述
                                </TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            <TableRow>
                                <TableCell>Book-10</TableCell>
                                <TableCell>对象缓存</TableCell>
                                <TableCell>对于被修改的书籍<code>Book-10</code>而言，其对象缓存失效</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>Book.store-10</TableCell>
                                <TableCell>关联缓存</TableCell>
                                <TableCell>对于被修改的书籍<code>Book-10</code>而言，其many-to-one关联属性<code>Book.store</code>所对应的缓存失效</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.books-2</TableCell>
                                <TableCell>关联缓存</TableCell>
                                <TableCell>对于旧的父对象<code>BookStore-2</code>而言，其one-to-many关联属性<code>BookStore.books</code>所对应的缓存失效</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell style={{whiteSpace: "nowrap"}}>BookStore.newestBooks-2</TableCell>
                                <TableCell>计算缓存</TableCell>
                                <TableCell>对于旧的父对象<code>BookStore-2</code>而言，其关联型计算属性<code>BookStore.newestBooks</code>所对应的缓存失效</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.avgPrice-2</TableCell>
                                <TableCell>计算缓存</TableCell>
                                <TableCell>对于旧的父对象<code>BookStore-2</code>而言，其关统计型计算属性<code>BookStore.avgPrice</code>所对应的缓存失效</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.books-1</TableCell>
                                <TableCell>关联缓存</TableCell>
                                <TableCell>对于新的父对象<code>BookStore-1</code>而言，其one-to-many关联属性<code>BookStore.books</code>所对应的缓存失效</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.newestBooks-1</TableCell>
                                <TableCell>计算缓存</TableCell>
                                <TableCell>对于新的父对象<code>BookStore-1</code>而言，其关联型计算属性<code>BookStore.newestBooks</code>所对应的缓存失效</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.avgPrice-1</TableCell>
                                <TableCell>计算缓存</TableCell>
                                <TableCell>对于新的父对象<code>BookStore-1</code>而言，其关统计型计算属性<code>BookStore.avgPrice</code>所对应的缓存失效</TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </TableContainer>
            </div>
        </p>
    </>;

const EN: ReactNode = 
    <>
        <p><img src={Consistency}/></p>
        <p>
            <b>Left side</b>: modify the data in the database, udpate the foreign key <code>STORE_ID</code> of <code>Book-10</code> from <code>2</code> to <code>1</code>
        </p>
        <p>
            <b>Right side</b>: This action will cause a chain reaction, jimmer will automatically clean up all affected caches, and the actions of cleaning the cache will leave log records in the IDE
            <div style={{padding: '1rem', marginLeft: '2rem'}}>
                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>
                                    The name of the cache to be cleared
                                </TableCell>
                                <TableCell>
                                    <span style={{whiteSpace: "nowrap"}}>cache type</span>
                                </TableCell>
                                <TableCell>
                                    describe
                                </TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            <TableRow>
                                <TableCell>Book-10</TableCell>
                                <TableCell>Object Cache</TableCell>
                                <TableCell>For the modified book <code>Book-10</code>, its object cache is invalid</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>Book.store-10</TableCell>
                                <TableCell>Association cache</TableCell>
                                <TableCell>For the modified book <code>Book-10</code>, the cache corresponding to the many-to-one association property <code>Book.store</code> is invalid</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.books-2</TableCell>
                                <TableCell>Association cache</TableCell>
                                <TableCell>For the old parent object <code>BookStore-2</code>, the cache corresponding to its one-to-many association property <code>BookStore.books</code> is invalid</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell style={{whiteSpace: "nowrap"}}>BookStore.newestBooks-2</TableCell>
                                <TableCell>Calculated cache</TableCell>
                                <TableCell>For the old parent object <code>BookStore-2</code>, the cache corresponding to the associated calculated property <code>BookStore.newestBooks</code> is invalid</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.avgPrice-2</TableCell>
                                <TableCell>Calculated cache</TableCell>
                                <TableCell>For the old parent object <code>BookStore-2</code>, the cache corresponding to the statistical calculated property <code>BookStore.avgPrice</code> is invalid</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.books-1</TableCell>
                                <TableCell>Association cache</TableCell>
                                <TableCell>For the new parent object <code>BookStore-1</code>, the cache corresponding to its one-to-many association property <code>BookStore.books</code> is invalid</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.newestBooks-1</TableCell>
                                <TableCell>Calculated cache</TableCell>
                                <TableCell>For the new parent object <code>BookStore-1</code>, the cache corresponding to the associated calculated property <code>BookStore.newestBooks</code> is invalid</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell>BookStore.avgPrice-1</TableCell>
                                <TableCell>Calculated cache</TableCell>
                                <TableCell>For the new parent object <code>BookStore-1</code>, the cache corresponding to the statistical calculated property <code>BookStore.avgPrice</code> is invalid</TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </TableContainer>
            </div>
        </p>
    </>;