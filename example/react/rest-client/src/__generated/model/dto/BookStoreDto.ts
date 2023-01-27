import type { Gender } from '../enums';

export type BookStoreDto = {
    'BookStoreService/SIMPLE_FETCHER': {
        readonly id: number, 
        readonly name: string
    }, 
    'BookStoreService/LIST_ITEM_FETCHER': {
        readonly id: number, 
        readonly createdTime: string, 
        readonly modifiedTime: string, 
        readonly name: string, 
        readonly website?: string
    }, 
    'BookStoreService/COMPLEX_FETCHER': {
        readonly id: number, 
        readonly createdTime: string, 
        readonly modifiedTime: string, 
        readonly name: string, 
        readonly website?: string, 
        readonly avgPrice: number, 
        readonly books: ReadonlyArray<{
            readonly id: number, 
            readonly createdTime: string, 
            readonly modifiedTime: string, 
            readonly name: string, 
            readonly edition: number, 
            readonly price: number, 
            readonly authors: ReadonlyArray<{
                readonly id: number, 
                readonly createdTime: string, 
                readonly modifiedTime: string, 
                readonly firstName: string, 
                readonly lastName: string, 
                readonly gender: Gender
            }>
        }>
    }
}