import type { Gender } from '../model/enums';
import type { AuthorDto } from '../model/dto';
import type { Executor } from '../';

export class AuthorService {
    
    constructor(private executor: Executor) {}
    
    async findAuthors(options: AuthorServiceOptions['findAuthors']): Promise<
        ReadonlyArray<AuthorDto['AuthorService/DEFAULT_FETCHER']>
    > {
        let uri = '/author/list';
        uri += '?sortCode=';
        uri += encodeURIComponent(options.sortCode);
        if (options.firstName !== undefined && options.firstName !== null) {
            uri += '&firstName=';
            uri += encodeURIComponent(options.firstName);
        }
        if (options.lastName !== undefined && options.lastName !== null) {
            uri += '&lastName=';
            uri += encodeURIComponent(options.lastName);
        }
        if (options.gender !== undefined && options.gender !== null) {
            uri += '&gender=';
            uri += encodeURIComponent(options.gender);
        }
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/DEFAULT_FETCHER']>
    }
    
    async findComplexAuthors(options: AuthorServiceOptions['findComplexAuthors']): Promise<
        ReadonlyArray<AuthorDto['AuthorService/COMPLEX_FETCHER']>
    > {
        let uri = '/author/complexList';
        uri += '?sortCode=';
        uri += encodeURIComponent(options.sortCode);
        if (options.firstName !== undefined && options.firstName !== null) {
            uri += '&firstName=';
            uri += encodeURIComponent(options.firstName);
        }
        if (options.lastName !== undefined && options.lastName !== null) {
            uri += '&lastName=';
            uri += encodeURIComponent(options.lastName);
        }
        if (options.gender !== undefined && options.gender !== null) {
            uri += '&gender=';
            uri += encodeURIComponent(options.gender);
        }
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/COMPLEX_FETCHER']>
    }
    
    async findSimpleAuthors(): Promise<
        ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    > {
        let uri = '/author/simpleList';
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    }
}

export type AuthorServiceOptions = {
    'findAuthors': {
        readonly sortCode: string, 
        readonly firstName?: string, 
        readonly lastName?: string, 
        readonly gender?: Gender
    },
    'findComplexAuthors': {
        readonly sortCode: string, 
        readonly firstName?: string, 
        readonly lastName?: string, 
        readonly gender?: Gender
    },
    'findSimpleAuthors': {}
}