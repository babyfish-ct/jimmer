import type { Book } from './';

export interface Chapter {
    
    readonly createdTime: string;
    
    readonly modifiedTime: string;
    
    readonly id: number;
    
    readonly book?: Book;
    
    readonly index: number;
    
    readonly title: string;
}