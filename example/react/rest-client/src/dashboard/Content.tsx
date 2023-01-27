import { FC, memo } from "react";
import { Route, Switch } from "wouter";

import { BookStoreList } from "../store/BookStoreList";
import { BookList } from "../book/BookList";

export const Content: FC = memo(() => {
    return (
        <Switch>
            <Route path="/bookStores" component={BookStoreList}/>
            <Route path="/books" component={BookList}/>
        </Switch>
    );
});