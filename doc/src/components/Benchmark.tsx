import React, { useCallback, useState } from "react";
import {
	G2,
	Chart,
	Tooltip,
	Interval,
	Interaction,
    Axis,
    Legend
} from "bizcharts";

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import FormGroup from '@mui/material/FormGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import Divider from '@mui/material/Divider';
import { ScaleOption } from "bizcharts/lib/interface";

export const Benchmark: React.FC<{
    readonly type: "OPS" | "TIME",
    readonly locale?: "zh"
}> = ({type, locale}) => {

    const [showJdbc, setShowJdbc] = useState(type !== "OPS");
    const onShowJdbcChange = useCallback((event: React.ChangeEvent<HTMLInputElement>, checked: boolean) => {
        setShowJdbc(checked);
    }, []);

    return (
        <Tabs groupId="chart-mode">
            <TabItem value="chart" label={locale === "zh" ? "图表" : "Chart"} default>
                { 
                    type === "OPS" && 
                    <>
                        <FormGroup>
                            <FormControlLabel 
                            control={<Checkbox value={showJdbc} onChange={onShowJdbcChange}/>} 
                            label={locale === "zh" ? "显示原生JDBC指标" : "Show native JDBC metrics"}/>
                        </FormGroup> 
                        <Divider/>
                    </>
                }
                {
                    type == "OPS" ?
                    <BenchmarkChart rows={opsRows} scale={opsScale} showJdbc={showJdbc}/> :
                    <BenchmarkChart rows={timeRows} scale={timeScale} showJdbc={showJdbc}/>
                }
            </TabItem>
            <TabItem value="data" label={locale === "zh" ? "数据" : "Data"}>
                {
                    type === "OPS" ?
                    <BenchmarkTable rows={sortedOpsRows} valueTitle="Ops/s"/> :
                    <BenchmarkTable rows={sortedTimeRows} valueTitle="Time(μs)"/>
                }
            </TabItem>
        </Tabs>
    );
};

const BenchmarkChart: React.FC<{
    readonly rows: ReadonlyArray<Row>,
    readonly scale: {
        readonly [field: string]: ScaleOption
    },
    readonly showJdbc: boolean
}> = ({rows, scale, showJdbc}) => {
    return (
        <Chart 
        filter={showJdbc ? undefined : {"framework": (v: any) => !(v as string).startsWith("JDBC")}}
        height={400} 
        padding="auto" 
        data={rows} 
        scale={scale} 
        autoFit>
			<Interval
				adjust={[
					{
						type: 'dodge',
						marginRatio: 0,
					},
				]}
				color="framework"
				position="dataCount*value"
			/>
            <Axis name="value" title/>
            <Legend position="left"/>
			<Tooltip shared />
			<Interaction type="active-region" />
		</Chart>
    );
}

const BenchmarkTable: React.FC<{readonly rows: ReadonlyArray<Row>, readonly valueTitle: string}> = ({rows, valueTitle}) => {
    return (
        <Table>
            <TableHead>
                <TableRow>
                    <TableCell>Framework</TableCell>
                    <TableCell>Data count</TableCell>
                    <TableCell>{valueTitle}</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {
                    rows.map(row => 
                        <TableRow key={`${row.framework}-${row.dataCount}`}>
                            <TableCell>{row.framework}</TableCell>
                            <TableCell>{row.dataCount}</TableCell>
                            <TableCell>{row.value}</TableCell>
                        </TableRow>
                    )
                }
            </TableBody>
        </Table>
    );
};

const opsScale = {
	dataCount: {
		alias: "Data count"
	},
	value: {
		alias: "Ops/s"
	}
};

const timeScale = {
	dataCount: {
		alias: "Data count"
	},
	value: {
		alias: 'Time(μs)'
	}
};

interface Row {
    readonly framework: string;
    readonly dataCount: string;
    readonly value: number;
};

const rows: ReadonlyArray<Row> = [
    {framework: "JDBC(ColIndex)", dataCount: "10", value: 668548},
	{framework: "JDBC(ColIndex)", dataCount: "20", value: 485607},
	{framework: "JDBC(ColIndex)", dataCount: "50", value: 260938},
	{framework: "JDBC(ColIndex)", dataCount: "100", value: 133789},
	{framework: "JDBC(ColIndex)", dataCount: "200", value: 71362},
	{framework: "JDBC(ColIndex)", dataCount: "500", value: 32131},
	{framework: "JDBC(ColIndex)", dataCount: "1000", value: 16088},
	{framework: "JDBC(ColName)", dataCount: "10", value: 340571},
	{framework: "JDBC(ColName)", dataCount: "20", value: 239359},
	{framework: "JDBC(ColName)", dataCount: "50", value: 128565},
	{framework: "JDBC(ColName)", dataCount: "100", value: 70058},
	{framework: "JDBC(ColName)", dataCount: "200", value: 38895},
	{framework: "JDBC(ColName)", dataCount: "500", value: 16152},
	{framework: "JDBC(ColName)", dataCount: "1000", value: 8172},
	{framework: "Jimmer(Java)", dataCount: "10", value: 348417},
	{framework: "Jimmer(Java)", dataCount: "20", value: 251418},
	{framework: "Jimmer(Java)", dataCount: "50", value: 135241},
	{framework: "Jimmer(Java)", dataCount: "100", value: 78190},
	{framework: "Jimmer(Java)", dataCount: "200", value: 41688},
	{framework: "Jimmer(Java)", dataCount: "500", value: 17355},
	{framework: "Jimmer(Java)", dataCount: "1000", value: 8815},
	{framework: "Jimmer(Kotlin)", dataCount: "10", value: 339465},
	{framework: "Jimmer(Kotlin)", dataCount: "20", value: 245564},
	{framework: "Jimmer(Kotlin)", dataCount: "50", value: 133790},
	{framework: "Jimmer(Kotlin)", dataCount: "100", value: 74563},
	{framework: "Jimmer(Kotlin)", dataCount: "200", value: 39329},
	{framework: "Jimmer(Kotlin)", dataCount: "500", value: 16647},
	{framework: "Jimmer(Kotlin)", dataCount: "1000", value: 8717},
	{framework: "EasyQuery", dataCount: "10", value: 225383},
	{framework: "EasyQuery", dataCount: "20", value: 144832},
	{framework: "EasyQuery", dataCount: "50", value: 76663},
	{framework: "EasyQuery", dataCount: "100", value: 39687},
	{framework: "EasyQuery", dataCount: "200", value: 20858},
	{framework: "EasyQuery", dataCount: "500", value: 8452},
	{framework: "EasyQuery", dataCount: "1000", value: 4511},
	{framework: "MyBatis", dataCount: "10", value: 75843},
	{framework: "MyBatis", dataCount: "20", value: 43330},
	{framework: "MyBatis", dataCount: "50", value: 19353},
	{framework: "MyBatis", dataCount: "100", value: 10430},
	{framework: "MyBatis", dataCount: "200", value: 5273},
	{framework: "MyBatis", dataCount: "500", value: 2099},
	{framework: "MyBatis", dataCount: "1000", value: 1070},
	{framework: "Exposed", dataCount: "10", value: 92778},
	{framework: "Exposed", dataCount: "20", value: 61642},
	{framework: "Exposed", dataCount: "50", value: 30785},
	{framework: "Exposed", dataCount: "100", value: 16518},
	{framework: "Exposed", dataCount: "200", value: 9050},
	{framework: "Exposed", dataCount: "500", value: 3831},
	{framework: "Exposed", dataCount: "1000", value: 1950},
	{framework: "JPA(Hibernate)", dataCount: "10", value: 102253},
	{framework: "JPA(Hibernate)", dataCount: "20", value: 59467},
	{framework: "JPA(Hibernate)", dataCount: "50", value: 26257},
	{framework: "JPA(Hibernate)", dataCount: "100", value: 13539},
	{framework: "JPA(Hibernate)", dataCount: "200", value: 6977},
	{framework: "JPA(Hibernate)", dataCount: "500", value: 2791},
	{framework: "JPA(Hibernate)", dataCount: "1000", value: 1396},
	{framework: "JPA(EclipseLink)", dataCount: "10", value: 64726},
	{framework: "JPA(EclipseLink)", dataCount: "20", value: 33448},
	{framework: "JPA(EclipseLink)", dataCount: "50", value: 13420},
	{framework: "JPA(EclipseLink)", dataCount: "100", value: 6473},
	{framework: "JPA(EclipseLink)", dataCount: "200", value: 3378},
	{framework: "JPA(EclipseLink)", dataCount: "500", value: 1362},
	{framework: "JPA(EclipseLink)", dataCount: "1000", value: 667},
	{framework: "JOOQ", dataCount: "10", value: 68624},
	{framework: "JOOQ", dataCount: "20", value: 35257},
	{framework: "JOOQ", dataCount: "50", value: 15514},
	{framework: "JOOQ", dataCount: "100", value: 7871},
	{framework: "JOOQ", dataCount: "200", value: 4856},
	{framework: "JOOQ", dataCount: "500", value: 1895},
	{framework: "JOOQ", dataCount: "1000", value: 879},
	{framework: "Nutz", dataCount: "10", value: 70429},
	{framework: "Nutz", dataCount: "20", value: 38999},
	{framework: "Nutz", dataCount: "50", value: 17200},
	{framework: "Nutz", dataCount: "100", value: 8268},
	{framework: "Nutz", dataCount: "200", value: 4629},
	{framework: "Nutz", dataCount: "500", value: 1859},
	{framework: "Nutz", dataCount: "1000", value: 957},
	{framework: "ObjectiveSQL", dataCount: "10", value: 47800},
	{framework: "ObjectiveSQL", dataCount: "20", value: 30930},
	{framework: "ObjectiveSQL", dataCount: "50", value: 13229},
	{framework: "ObjectiveSQL", dataCount: "100", value: 6633},
	{framework: "ObjectiveSQL", dataCount: "200", value: 3289},
	{framework: "ObjectiveSQL", dataCount: "500", value: 1338},
	{framework: "ObjectiveSQL", dataCount: "1000", value: 617},
	{framework: "Spring Data JDBC", dataCount: "10", value: 20647},
	{framework: "Spring Data JDBC", dataCount: "20", value: 10672},
	{framework: "Spring Data JDBC", dataCount: "50", value: 4452},
	{framework: "Spring Data JDBC", dataCount: "100", value: 2032},
	{framework: "Spring Data JDBC", dataCount: "200", value: 1084},
	{framework: "Spring Data JDBC", dataCount: "500", value: 460},
	{framework: "Spring Data JDBC", dataCount: "1000", value: 233},
	{framework: "Ktorm", dataCount: "10", value: 18816},
	{framework: "Ktorm", dataCount: "20", value: 10157},
	{framework: "Ktorm", dataCount: "50", value: 4185},
	{framework: "Ktorm", dataCount: "100", value: 2137},
	{framework: "Ktorm", dataCount: "200", value: 1036},
	{framework: "Ktorm", dataCount: "500", value: 361},
	{framework: "Ktorm", dataCount: "1000", value: 229},
];

const opsRows = rows.map(item => ({...item, value: Math.round(item.value)}));

const timeRows = rows.map(item => ({...item, value: Math.round(1000000 / item.value)}));

function compareByDataCount(a: Row, b: Row) : number {
    return parseInt(a.dataCount) - parseInt(b.dataCount);
}

const sortedOpsRows = opsRows.sort(compareByDataCount);

const sortedTimeRows = timeRows.sort(compareByDataCount);

