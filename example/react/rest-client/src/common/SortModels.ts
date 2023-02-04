import { GridSelectionModel, GridSortDirection, GridSortModel } from "@mui/x-data-grid";

export function toSortModel(sortCode: string): GridSortModel {
    return sortCode.split(/\s*[,;]\s*/).filter(part => part.length != 0).map(part => {
        const words = part.split(/\s+/);
        return { field: words[0], sort: words[1] as GridSortDirection };
    });
}

export function toSortCode(sortModel: GridSortModel): string {
    let code = "";
    for (const item of sortModel) {
        code += item.field;
        if (item.sort) {
            code += ' ';
            code += item.sort;
        }
        code += ", ";
    }
    return code;
}