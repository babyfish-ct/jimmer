import { FC, memo } from "react";
import { Route, Switch } from "wouter";

import { SimpleBookStoreList } from "../store/SimpleBookStoreList";
import { ComplexBookStoreList } from "../store/ComplexBookStoreList";
import { SimpleBookList } from "../book/SimpleBookList";

export const Content: FC = memo(() => {
    return (
        <Switch>
            <Route path="/simpleBookStores" component={SimpleBookStoreList}/>
            <Route path="/complexBookStores" component={ComplexBookStoreList}/>
            <Route path="/simpleBooks" component={SimpleBookList}/>
        </Switch>
    );
});