import type { Gender } from '../model/enums';
import type { AuthorDto } from '../model/dto';
import type { Executor } from '../';

export class AuthorService {
    
    constructor(private executor: Executor) {}
    
    async findComplexAuthors(options?: AuthorServiceOptions['findComplexAuthors']): Promise<
        ReadonlyArray<AuthorDto['AuthorService/COMPLEX_FETCHER']>
    > {
        let uri = '/authors/complex';
        let separator = '?';
        if (options?.firstName !== undefined && options.firstName !== null) {
            uri += separator;
            uri += 'firstName=';
            uri += encodeURIComponent(options.firstName);
            separator = '&';
        }
        if (options?.lastName !== undefined && options.lastName !== null) {
            uri += separator;
            uri += 'lastName=';
            uri += encodeURIComponent(options.lastName);
            separator = '&';
        }
        if (options?.gender !== undefined && options.gender !== null) {
            uri += separator;
            uri += 'gender=';
            uri += encodeURIComponent(options.gender);
            separator = '&';
        }
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/COMPLEX_FETCHER']>
    }
    
    async findSimpleAuthors(options?: AuthorServiceOptions['findSimpleAuthors']): Promise<
        ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    > {
        let uri = '/authors/simple';
        let separator = '?';
        if (options?.firstName !== undefined && options.firstName !== null) {
            uri += separator;
            uri += 'firstName=';
            uri += encodeURIComponent(options.firstName);
            separator = '&';
        }
        if (options?.lastName !== undefined && options.lastName !== null) {
            uri += separator;
            uri += 'lastName=';
            uri += encodeURIComponent(options.lastName);
            separator = '&';
        }
        if (options?.gender !== undefined && options.gender !== null) {
            uri += separator;
            uri += 'gender=';
            uri += encodeURIComponent(options.gender);
            separator = '&';
        }
        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>
    }
}

export type AuthorServiceOptions = {
    'findComplexAuthors': {
        readonly firstName?: string, 
        readonly lastName?: string, 
        readonly gender?: Gender
    },
    'findSimpleAuthors': {
        readonly firstName?: string, 
        readonly lastName?: string, 
        readonly gender?: Gender
    }
}